package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.execution.ExecutionStatus;
import com.interview.interview.CodeSubmittedEvent;
import com.interview.interview.domain.CheckpointResultStatus;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 設計說明：獨立 @Service + @Async 方法，負責非同步執行測試並評分。
 * Spring proxy 無法攔截 self-invocation，必須拆成獨立 bean（與 ContainerInitializationService 同模式）。
 * submitCode() 在事務提交後由此 bean 觸發背景執行，確保 IN_PROGRESS 狀態已持久化。
 */
@Service
public class CheckpointTestExecutor {

    private static final Logger log = LoggerFactory.getLogger(CheckpointTestExecutor.class);

    private final ContainerService containerService;
    private final CheckpointResultRepository checkpointResultRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InterviewService interviewService;

    public CheckpointTestExecutor(ContainerService containerService,
                                  CheckpointResultRepository checkpointResultRepository,
                                  ApplicationEventPublisher eventPublisher,
                                  InterviewService interviewService) {
        this.containerService = containerService;
        this.checkpointResultRepository = checkpointResultRepository;
        this.eventPublisher = eventPublisher;
        this.interviewService = interviewService;
    }

    /**
     * Asynchronously executes the test command in the container and grades the result.
     * Runs in a background thread after submitCode() transaction commits.
     * On any failure, marks the checkpoint as FAILED to prevent stuck IN_PROGRESS.
     */
    @Async
    public void executeAndGrade(UUID interviewId, String checkpointId,
                                int checkpointSequence,
                                String containerId, String fullCommand) {
        try {
            log.info("interview={} checkpoint={} async test execution starting",
                    interviewId, checkpointId);
            var execResult = containerService.execCommand(containerId, fullCommand, 300);

            var result = checkpointResultRepository
                    .findByInterviewIdAndCheckpointId(interviewId, checkpointId)
                    .orElseThrow(() -> new IllegalStateException(
                            "CheckpointResult not found for interview=" + interviewId + " checkpoint=" + checkpointId));

            String output = formatOutput(execResult.stdout(), execResult.stderr(), execResult.exitCode());
            boolean passed = execResult.status() == ExecutionStatus.SUCCESS || execResult.exitCode() == 0;
            log.info("interview={} checkpoint={} test exitCode={} passed={}",
                    interviewId, checkpointId, execResult.exitCode(), passed);

            if (passed) {
                result.markPassed(output);
            } else {
                result.markFailed(output);
            }
            checkpointResultRepository.save(result);

            eventPublisher.publishEvent(new CodeSubmittedEvent(
                    interviewId, checkpointId, checkpointSequence, passed));

            if (passed) {
                checkIfAllCheckpointsPassed(interviewId);
            }
        } catch (Exception e) {
            log.error("interview={} checkpoint={} async test execution failed: {}",
                    interviewId, checkpointId, e.getMessage(), e);
            // 防止 checkpoint 永遠卡在 IN_PROGRESS
            checkpointResultRepository
                    .findByInterviewIdAndCheckpointId(interviewId, checkpointId)
                    .ifPresent(r -> {
                        try {
                            r.markFailed("Internal execution error: " + e.getMessage());
                            checkpointResultRepository.save(r);
                        } catch (Exception saveEx) {
                            log.error("interview={} checkpoint={} failed to mark checkpoint as FAILED: {}",
                                    interviewId, checkpointId, saveEx.getMessage(), saveEx);
                        }
                    });
        }
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
        var allResults = checkpointResultRepository.findAllByInterviewId(interviewId);
        boolean allPassed = allResults.stream()
                .allMatch(r -> r.getCheckpointResultStatus() == CheckpointResultStatus.PASSED);
        if (allPassed) {
            log.info("interview={} all checkpoints passed, completing interview", interviewId);
            interviewService.completeInterview(interviewId);
        }
    }
}
