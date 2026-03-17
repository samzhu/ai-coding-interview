package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.execution.ExamConfig;
import com.interview.execution.ExamConfigService;
import com.interview.interview.InterviewExpiredException;
import com.interview.interview.InterviewTimeProvider;
import com.interview.interview.domain.CheckpointResult;
import com.interview.interview.domain.InterviewStatus;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CheckpointProgressService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointProgressService.class);

    private final InterviewRepository interviewRepository;
    private final CheckpointResultRepository checkpointResultRepository;
    private final ContainerService containerService;
    private final InterviewService interviewService;
    private final ExamConfigService examConfigService;
    private final InterviewTimeProvider interviewTimeProvider;
    private final CheckpointTestExecutor checkpointTestExecutor;

    public CheckpointProgressService(InterviewRepository interviewRepository,
                                     CheckpointResultRepository checkpointResultRepository,
                                     ContainerService containerService,
                                     InterviewService interviewService,
                                     ExamConfigService examConfigService,
                                     InterviewTimeProvider interviewTimeProvider,
                                     CheckpointTestExecutor checkpointTestExecutor) {
        this.interviewRepository = interviewRepository;
        this.checkpointResultRepository = checkpointResultRepository;
        this.containerService = containerService;
        this.interviewService = interviewService;
        this.examConfigService = examConfigService;
        this.interviewTimeProvider = interviewTimeProvider;
        this.checkpointTestExecutor = checkpointTestExecutor;
    }

    @Transactional(readOnly = true)
    public CheckpointView getCurrentCheckpoint(UUID interviewId) {
        var interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        var result = checkpointResultRepository.findCurrentCheckpoint(interviewId)
                .orElseThrow(() -> new IllegalStateException("No active checkpoint for interview: " + interviewId));

        String containerId = interview.getContainerId();
        if (containerId == null || containerId.isBlank()) {
            throw new IllegalStateException("No container assigned for interview: " + interviewId);
        }
        var examConfig = examConfigService.getExamConfig(containerId);
        var checkpoint = findCheckpoint(examConfig, result.getCheckpointId());
        return new CheckpointView(result, checkpoint, null);
    }

    @Transactional(readOnly = true)
    public List<CheckpointView> getAllCheckpoints(UUID interviewId) {
        var interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        var results = checkpointResultRepository.findAllByInterviewId(interviewId);
        if (results.isEmpty()) {
            return List.of();
        }

        String containerId = interview.getContainerId();
        if (containerId == null || containerId.isBlank()) {
            // 容器尚未建立，直接回傳無標題的 view（graceful degradation）
            return results.stream()
                    .sorted(Comparator.comparingInt(CheckpointResult::getCheckpointSequence))
                    .map(r -> new CheckpointView(r.getCheckpointId(), r.getCheckpointSequence(),
                            "", "", null, null, r.getStatus(), r.getSubmittedCode(),
                            r.getExecutionOutput(), r.getPassedAt(), null))
                    .toList();
        }

        var examConfig = examConfigService.getExamConfig(containerId);
        return results.stream()
                .sorted(Comparator.comparingInt(CheckpointResult::getCheckpointSequence))
                .map(r -> {
                    var cp = examConfig.checkpoints().stream()
                            .filter(c -> c.id().equals(r.getCheckpointId()))
                            .findFirst()
                            .orElse(null);
                    if (cp == null) {
                        return new CheckpointView(r.getCheckpointId(), r.getCheckpointSequence(),
                                "", "", null, null, r.getStatus(), r.getSubmittedCode(),
                                r.getExecutionOutput(), r.getPassedAt(), null);
                    }
                    return new CheckpointView(r, cp, null);
                })
                .toList();
    }

    public CheckpointView submitCode(SubmitCodeCommand command) {
        log.info("interview={} checkpoint={} submit starting", command.interviewId(), command.checkpointId());

        var interview = interviewRepository.findById(command.interviewId())
                .orElseThrow(() -> {
                    log.warn("interview={} not found", command.interviewId());
                    return new IllegalArgumentException("Interview not found: " + command.interviewId());
                });

        if (interview.getInterviewStatus() != InterviewStatus.IN_PROGRESS) {
            log.warn("interview={} submit rejected — status={}", command.interviewId(), interview.getInterviewStatus());
            throw new IllegalStateException("Interview is not in progress: " + command.interviewId());
        }

        if (interviewTimeProvider.isExpired(command.interviewId())) {
            log.warn("interview={} submit rejected — interview expired", command.interviewId());
            try {
                interviewService.completeInterview(command.interviewId());
            } catch (Exception ignored) {
            }
            throw new InterviewExpiredException("面試時間已到，無法提交程式碼");
        }

        var result = checkpointResultRepository
                .findByInterviewIdAndCheckpointId(command.interviewId(), command.checkpointId())
                .orElseThrow(() -> {
                    log.warn("interview={} checkpoint={} not found in repository", command.interviewId(), command.checkpointId());
                    return new IllegalArgumentException("Checkpoint not found for this interview");
                });

        log.info("interview={} checkpoint={} ensuring container running", command.interviewId(), command.checkpointId());
        String containerId = interviewService.ensureContainerRunning(interview.getId());
        log.info("interview={} checkpoint={} container ready, containerId={}", command.interviewId(), command.checkpointId(), containerId);

        var examConfig = examConfigService.getExamConfig(containerId);
        var checkpoint = findCheckpoint(examConfig, command.checkpointId());

        result.startProgress("submitted");
        checkpointResultRepository.save(result);

        String fullCommand = "cd " + examConfig.effectiveWorkspace() + " && " + checkpoint.testCommand();

        UUID processId = UUID.randomUUID();
        // 委派給背景執行緒非同步執行測試，避免 proxy timeout（Next.js ~30s < Docker exec 300s）
        checkpointTestExecutor.executeAndGrade(
                processId,
                command.interviewId(), command.checkpointId(),
                result.getCheckpointSequence(),
                containerId, fullCommand);

        // 立即返回 IN_PROGRESS 狀態與 processId，前端透過 processId polling 取得即時輸出與最終結果
        return new CheckpointView(result, checkpoint, processId);
    }

    public CheckpointView runTests(UUID interviewId, String checkpointId) {
        log.info("interview={} checkpoint={} runTests starting", interviewId, checkpointId);

        var interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));

        if (interviewTimeProvider.isExpired(interviewId)) {
            log.warn("interview={} runTests rejected — interview expired", interviewId);
            throw new InterviewExpiredException("面試時間已到，無法執行測試");
        }

        var result = checkpointResultRepository
                .findByInterviewIdAndCheckpointId(interviewId, checkpointId)
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found for this interview"));

        log.info("interview={} checkpoint={} ensuring container running", interviewId, checkpointId);
        String containerId = interviewService.ensureContainerRunning(interviewId);
        log.info("interview={} checkpoint={} container ready, containerId={}", interviewId, checkpointId, containerId);

        var examConfig = examConfigService.getExamConfig(containerId);
        var checkpoint = findCheckpoint(examConfig, checkpointId);

        String fullCommand = "cd " + examConfig.effectiveWorkspace() + " && " + checkpoint.testCommand();
        log.info("interview={} checkpoint={} executing test command", interviewId, checkpointId);
        var execResult = containerService.execCommand(containerId, fullCommand, 300);
        log.info("interview={} checkpoint={} runTests exitCode={}", interviewId, checkpointId, execResult.exitCode());
        String output = formatOutput(execResult.stdout(), execResult.stderr(), execResult.exitCode());

        return new CheckpointView(
                result.getCheckpointId(),
                result.getCheckpointSequence(),
                checkpoint.title(),
                checkpoint.description(),
                null,
                checkpoint.testCommand(),
                result.getStatus(),
                result.getSubmittedCode(),
                output,
                result.getPassedAt(),
                null);
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

    private ExamConfig.ExamCheckpoint findCheckpoint(ExamConfig examConfig, String checkpointId) {
        return examConfig.checkpoints().stream()
                .filter(cp -> cp.id().equals(checkpointId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Checkpoint '" + checkpointId + "' not found in exam config"));
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
            Instant passedAt,
            UUID processId) {

        /** ExamConfig 版本的便利建構子，sequenceNumber 從 CheckpointResult 取得 */
        public CheckpointView(CheckpointResult result, ExamConfig.ExamCheckpoint checkpoint, UUID processId) {
            this(
                    result.getCheckpointId(),
                    result.getCheckpointSequence(),
                    checkpoint.title(),
                    checkpoint.description(),
                    null,
                    checkpoint.testCommand(),
                    result.getStatus(),
                    result.getSubmittedCode(),
                    result.getExecutionOutput(),
                    result.getPassedAt(),
                    processId);
        }
    }
}
