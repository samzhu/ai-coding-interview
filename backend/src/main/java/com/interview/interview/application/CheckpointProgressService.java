package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.execution.ExecutionStatus;
import com.interview.interview.CodeSubmittedEvent;
import com.interview.interview.InterviewExpiredException;
import com.interview.interview.InterviewTimeProvider;
import com.interview.interview.domain.CheckpointResult;
import com.interview.interview.domain.CheckpointResultStatus;
import com.interview.interview.domain.InterviewStatus;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import com.interview.question.CheckpointDetail;
import com.interview.question.QuestionDetail;
import com.interview.question.QuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CheckpointProgressService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointProgressService.class);

    private final InterviewRepository interviewRepository;
    private final CheckpointResultRepository checkpointResultRepository;
    private final QuestionService questionService;
    private final ContainerService containerService;
    private final InterviewService interviewService;
    private final InterviewTimeProvider interviewTimeProvider;
    private final ApplicationEventPublisher eventPublisher;

    public CheckpointProgressService(InterviewRepository interviewRepository,
                                     CheckpointResultRepository checkpointResultRepository,
                                     QuestionService questionService,
                                     ContainerService containerService,
                                     InterviewService interviewService,
                                     InterviewTimeProvider interviewTimeProvider,
                                     ApplicationEventPublisher eventPublisher) {
        this.interviewRepository = interviewRepository;
        this.checkpointResultRepository = checkpointResultRepository;
        this.questionService = questionService;
        this.containerService = containerService;
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
        return new CheckpointView(result, checkpoint);
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
            }
            throw new InterviewExpiredException("面試時間已到，無法提交程式碼");
        }

        var result = checkpointResultRepository
                .findByInterviewIdAndCheckpointId(command.interviewId(), command.checkpointId())
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found for this interview"));

        var question = questionService.getQuestion(interview.getQuestionId());
        var checkpoint = findCheckpoint(question, command.checkpointId());

        result.startProgress("submitted");
        checkpointResultRepository.save(result);

        String containerId = interview.getContainerId();
        String workspace = question.workspace() != null ? question.workspace() : "/workspace";
        String fullCommand = "cd " + workspace + " && " + checkpoint.testCommand();

        var execResult = containerService.execCommand(containerId, fullCommand, 60);

        String output = formatOutput(execResult.stdout(), execResult.stderr(), execResult.exitCode());

        if (execResult.status() == ExecutionStatus.SUCCESS || execResult.exitCode() == 0) {
            result.markPassed(output);
        } else {
            result.markFailed(output);
        }

        checkpointResultRepository.save(result);

        boolean passed = result.getCheckpointResultStatus() == CheckpointResultStatus.PASSED;
        eventPublisher.publishEvent(new CodeSubmittedEvent(
                command.interviewId(), command.checkpointId(), checkpoint.sequenceNumber(), passed));

        if (passed) {
            checkIfAllCheckpointsPassed(command.interviewId());
        }

        return new CheckpointView(result, checkpoint);
    }

    public CheckpointView runTests(UUID interviewId, String checkpointId) {
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

        String containerId = interview.getContainerId();
        String workspace = question.workspace() != null ? question.workspace() : "/workspace";
        String fullCommand = "cd " + workspace + " && " + checkpoint.testCommand();

        var execResult = containerService.execCommand(containerId, fullCommand, 60);
        String output = formatOutput(execResult.stdout(), execResult.stderr(), execResult.exitCode());

        return new CheckpointView(
                result.getCheckpointId(),
                checkpoint.sequenceNumber(),
                checkpoint.title(),
                checkpoint.description(),
                checkpoint.starterCode(),
                checkpoint.testCommand(),
                result.getStatus(),
                result.getSubmittedCode(),
                output,
                result.getPassedAt());
    }

    private CheckpointDetail findCheckpoint(QuestionDetail question, String checkpointId) {
        return question.checkpoints().stream()
                .filter(cp -> cp.id().equals(checkpointId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found: " + checkpointId));
    }

    private String formatOutput(String stdout, String stderr, int exitCode) {
        var sb = new StringBuilder();
        if (stdout != null && !stdout.isBlank()) sb.append(stdout);
        if (stderr != null && !stderr.isBlank()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(stderr);
        }
        if (exitCode != 0 && sb.isEmpty()) {
            sb.append("Tests failed with exit code: ").append(exitCode);
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

    public record CheckpointView(
            String checkpointId,
            int sequenceNumber,
            String title,
            String description,
            String starterCode,
            String testCommand,
            String status,
            String submittedCode,
            String executionOutput,
            Instant passedAt) {

        public CheckpointView(CheckpointResult result, CheckpointDetail checkpoint) {
            this(
                    result.getCheckpointId(),
                    checkpoint.sequenceNumber(),
                    checkpoint.title(),
                    checkpoint.description(),
                    checkpoint.starterCode(),
                    checkpoint.testCommand(),
                    result.getStatus(),
                    result.getSubmittedCode(),
                    result.getExecutionOutput(),
                    result.getPassedAt());
        }
    }
}
