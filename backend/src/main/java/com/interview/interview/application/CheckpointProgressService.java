package com.interview.interview.application;

import com.interview.execution.CodeExecutionRequest;
import com.interview.execution.CodeExecutionService;
import com.interview.execution.ExecutionStatus;
import com.interview.execution.ProjectExecutionRequest;
import com.interview.interview.CodeSubmittedEvent;
import com.interview.interview.InterviewExpiredException;
import com.interview.interview.InterviewTimeProvider;
import com.interview.interview.domain.CheckpointResult;
import com.interview.interview.domain.CheckpointResultStatus;
import com.interview.interview.domain.InterviewStatus;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import com.interview.question.CheckpointDetail;
import com.interview.question.ProjectFileDetail;
import com.interview.question.QuestionDetail;
import com.interview.question.QuestionService;
import com.interview.question.TestCaseDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CheckpointProgressService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointProgressService.class);

    private final InterviewRepository interviewRepository;
    private final CheckpointResultRepository checkpointResultRepository;
    private final QuestionService questionService;
    private final CodeExecutionService codeExecutionService;
    private final InterviewService interviewService;
    private final InterviewTimeProvider interviewTimeProvider;
    private final ApplicationEventPublisher eventPublisher;

    public CheckpointProgressService(InterviewRepository interviewRepository,
                                     CheckpointResultRepository checkpointResultRepository,
                                     QuestionService questionService,
                                     CodeExecutionService codeExecutionService,
                                     InterviewService interviewService,
                                     InterviewTimeProvider interviewTimeProvider,
                                     ApplicationEventPublisher eventPublisher) {
        this.interviewRepository = interviewRepository;
        this.checkpointResultRepository = checkpointResultRepository;
        this.questionService = questionService;
        this.codeExecutionService = codeExecutionService;
        this.interviewService = interviewService;
        this.interviewTimeProvider = interviewTimeProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public CheckpointView getCurrentCheckpoint(UUID interviewId) {
        var interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        var result = checkpointResultRepository.findCurrentCheckpoint(interviewId)
                .orElseThrow(() -> new IllegalStateException("No active checkpoint for interview: " + interviewId));
        var question = questionService.getQuestion(interview.getQuestionId());
        var checkpoint = findCheckpoint(question, result.getCheckpointId());
        return new CheckpointView(result, checkpoint, question.projectFiles());
    }

    public CheckpointView submitCode(SubmitCodeCommand command) {
        var interview = interviewRepository.findById(command.interviewId())
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + command.interviewId()));

        if (interview.getInterviewStatus() != InterviewStatus.IN_PROGRESS) {
            throw new IllegalStateException("Interview is not in progress: " + command.interviewId());
        }

        if (interviewTimeProvider.isExpired(command.interviewId())) {
            try {
                interviewService.completeInterview(command.interviewId());
            } catch (Exception ignored) {
                // 面試可能已被完成，忽略
            }
            throw new InterviewExpiredException("面試時間已到，無法提交程式碼");
        }

        var result = checkpointResultRepository
                .findByInterviewIdAndCheckpointId(command.interviewId(), command.checkpointId())
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found for this interview"));

        var question = questionService.getQuestion(interview.getQuestionId());
        var checkpoint = findCheckpoint(question, command.checkpointId());

        if (checkpoint.testCommand() != null) {
            // Multi-file project mode: run test command
            executeProjectMode(result, question, checkpoint, command);
        } else {
            // Legacy single-file mode: run test cases
            executeLegacyMode(result, question, checkpoint, command);
        }

        checkpointResultRepository.save(result);

        boolean passed = result.getCheckpointResultStatus() == CheckpointResultStatus.PASSED;
        eventPublisher.publishEvent(new CodeSubmittedEvent(
                command.interviewId(), command.checkpointId(), checkpoint.sequenceNumber(), passed));

        if (passed) {
            checkIfAllCheckpointsPassed(command.interviewId());
        }

        return new CheckpointView(result, checkpoint, question.projectFiles());
    }

    public CheckpointView runTests(UUID interviewId, UUID checkpointId, Map<String, String> files) {
        var interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));

        if (interviewTimeProvider.isExpired(interviewId)) {
            throw new InterviewExpiredException("面試時間已到，無法執行測試");
        }

        var result = checkpointResultRepository
                .findByInterviewIdAndCheckpointId(interviewId, checkpointId)
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found for this interview"));

        var question = questionService.getQuestion(interview.getQuestionId());
        var checkpoint = findCheckpoint(question, checkpointId);

        String output;
        if (checkpoint.testCommand() != null) {
            Map<String, String> allFiles = mergeFiles(question.projectFiles(), files != null ? files : Map.of());
            var execRequest = new ProjectExecutionRequest(allFiles, checkpoint.testCommand(), 30);
            var execResult = codeExecutionService.executeProject(execRequest);
            output = formatProjectOutput(execResult.stdout(), execResult.stderr(), execResult.exitCode());
        } else {
            String code = files != null ? files.values().stream().findFirst().orElse("") : "";
            output = runTestCases(question.language(), code, checkpoint.testCases());
        }

        // Create a temporary view with run output without changing checkpoint result status
        return new CheckpointView(
                result.getCheckpointId(),
                checkpoint.sequenceNumber(),
                checkpoint.title(),
                checkpoint.description(),
                checkpoint.starterCode(),
                checkpoint.testCommand(),
                toProjectFileViews(question.projectFiles()),
                result.getStatus(),
                result.getSubmittedCode(),
                output,
                result.getPassedAt(),
                checkpoint.aiEnabled());
    }

    private void executeProjectMode(CheckpointResult result, QuestionDetail question,
                                    CheckpointDetail checkpoint, SubmitCodeCommand command) {
        Map<String, String> submittedFiles = command.files() != null ? command.files() : Map.of();
        result.startProgress(serializeFiles(submittedFiles));
        checkpointResultRepository.save(result);

        Map<String, String> allFiles = mergeFiles(question.projectFiles(), submittedFiles);
        var execRequest = new ProjectExecutionRequest(allFiles, checkpoint.testCommand(), 30);
        var execResult = codeExecutionService.executeProject(execRequest);

        String output = formatProjectOutput(execResult.stdout(), execResult.stderr(), execResult.exitCode());

        if (execResult.exitCode() == 0) {
            result.markPassed(output);
        } else {
            result.markFailed(output);
        }
    }

    private void executeLegacyMode(CheckpointResult result, QuestionDetail question,
                                   CheckpointDetail checkpoint, SubmitCodeCommand command) {
        String code = command.code() != null ? command.code() : "";
        result.startProgress(code);
        checkpointResultRepository.save(result);

        String executionOutput = runTestCases(question.language(), code, checkpoint.testCases());
        boolean allPassed = evaluateTestCases(question.language(), code, checkpoint.testCases());

        if (allPassed) {
            result.markPassed(executionOutput);
        } else {
            result.markFailed(executionOutput);
        }
    }

    private CheckpointDetail findCheckpoint(QuestionDetail question, UUID checkpointId) {
        return question.checkpoints().stream()
                .filter(cp -> cp.id().equals(checkpointId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found: " + checkpointId));
    }

    private Map<String, String> mergeFiles(List<ProjectFileDetail> projectFiles, Map<String, String> submittedFiles) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (ProjectFileDetail pf : projectFiles) {
            if (pf.editable() && submittedFiles.containsKey(pf.filePath())) {
                merged.put(pf.filePath(), submittedFiles.get(pf.filePath()));
            } else {
                merged.put(pf.filePath(), pf.content());
            }
        }
        return merged;
    }

    private String serializeFiles(Map<String, String> files) {
        if (files == null || files.isEmpty()) return "{}";
        var sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : files.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey().replace("\"", "\\\"")).append("\":");
            sb.append("\"").append(entry.getValue().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String formatProjectOutput(String stdout, String stderr, int exitCode) {
        var sb = new StringBuilder();
        if (!stdout.isBlank()) sb.append(stdout);
        if (!stderr.isBlank()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(stderr);
        }
        if (exitCode != 0 && sb.isEmpty()) {
            sb.append("Tests failed with exit code: ").append(exitCode);
        }
        return sb.toString();
    }

    private boolean evaluateTestCases(String language, String code, List<TestCaseDetail> testCases) {
        for (TestCaseDetail testCase : testCases) {
            var execRequest = new CodeExecutionRequest(language, code, testCase.input(), 10);
            var execResult = codeExecutionService.execute(execRequest);
            if (execResult.status() != ExecutionStatus.SUCCESS ||
                    !execResult.stdout().trim().equals(testCase.expectedOutput().trim())) {
                return false;
            }
        }
        return true;
    }

    private String runTestCases(String language, String code, List<TestCaseDetail> testCases) {
        var sb = new StringBuilder();
        for (TestCaseDetail testCase : testCases) {
            var execRequest = new CodeExecutionRequest(language, code, testCase.input(), 10);
            var execResult = codeExecutionService.execute(execRequest);
            boolean passed = execResult.status() == ExecutionStatus.SUCCESS &&
                    execResult.stdout().trim().equals(testCase.expectedOutput().trim());
            if (!testCase.isHidden()) {
                sb.append(passed ? "PASS" : "FAIL")
                  .append(" | input: ").append(testCase.input().replace("\n", "\\n"))
                  .append(" | expected: ").append(testCase.expectedOutput())
                  .append(" | got: ").append(execResult.stdout())
                  .append("\n");
            }
        }
        return sb.toString();
    }

    private void checkIfAllCheckpointsPassed(UUID interviewId) {
        List<CheckpointResult> allResults = checkpointResultRepository.findAllByInterviewId(interviewId);
        boolean allPassed = allResults.stream()
                .allMatch(r -> r.getCheckpointResultStatus() == CheckpointResultStatus.PASSED);
        if (allPassed) {
            interviewService.completeInterview(interviewId);
        }
    }

    private List<CheckpointView.ProjectFileView> toProjectFileViews(List<ProjectFileDetail> projectFiles) {
        return projectFiles.stream()
                .map(f -> new CheckpointView.ProjectFileView(f.filePath(), f.content(), f.editable()))
                .toList();
    }

    public record CheckpointView(
            UUID checkpointId,
            int sequenceNumber,
            String title,
            String description,
            String starterCode,
            String testCommand,
            List<ProjectFileView> projectFiles,
            String status,
            String submittedCode,
            String executionOutput,
            Instant passedAt,
            boolean aiEnabled) {

        public record ProjectFileView(String filePath, String content, boolean editable) {
        }

        public CheckpointView(CheckpointResult result, CheckpointDetail checkpoint, List<ProjectFileDetail> projectFiles) {
            this(
                    result.getCheckpointId(),
                    checkpoint.sequenceNumber(),
                    checkpoint.title(),
                    checkpoint.description(),
                    checkpoint.starterCode(),
                    checkpoint.testCommand(),
                    projectFiles.stream()
                            .map(f -> new ProjectFileView(f.filePath(), f.content(), f.editable()))
                            .toList(),
                    result.getStatus(),
                    result.getSubmittedCode(),
                    result.getExecutionOutput(),
                    result.getPassedAt(),
                    checkpoint.aiEnabled());
        }
    }
}
