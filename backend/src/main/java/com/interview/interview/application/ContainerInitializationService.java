package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.execution.ExamConfig;
import com.interview.execution.ExamConfigNotFoundException;
import com.interview.execution.ExamConfigService;
import com.interview.interview.InterviewStartedEvent;
import com.interview.interview.domain.CheckpointResult;
import com.interview.interview.domain.Interview;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import com.interview.question.QuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles the asynchronous container initialization phase of an interview start.
 *
 * Separated into its own bean so that the @Async annotation can be honoured by
 * Spring's proxy — self-invocation inside InterviewService would bypass the proxy
 * and run synchronously on the HTTP thread, potentially blocking for minutes while
 * a Docker image is being pulled.
 *
 * Flow (runs in background thread after startInterview() transaction commits):
 *   1. pull image if absent  (may take minutes)
 *   2. create + start container
 *   3. persist containerId to interview
 *   4. read exam.yml + initialise checkpoint results
 *   5. set containerStatus = "READY" + publish InterviewStartedEvent
 *
 *   On any failure: set containerStatus = "FAILED" so the frontend can surface an error.
 */
@Service
public class ContainerInitializationService {

    private static final Logger log = LoggerFactory.getLogger(ContainerInitializationService.class);

    private final InterviewRepository repository;
    private final QuestionService questionService;
    private final ContainerService containerService;
    private final ExamConfigService examConfigService;
    private final CheckpointResultRepository checkpointResultRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ContainerInitializationService(InterviewRepository repository,
                                          QuestionService questionService,
                                          ContainerService containerService,
                                          ExamConfigService examConfigService,
                                          CheckpointResultRepository checkpointResultRepository,
                                          ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.questionService = questionService;
        this.containerService = containerService;
        this.examConfigService = examConfigService;
        this.checkpointResultRepository = checkpointResultRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Asynchronously pulls the Docker image (if not cached), starts the container,
     * reads exam.yml, initialises checkpoints, and marks the interview as READY.
     *
     * Each individual repository.save() call runs in its own transaction (Spring Data
     * JDBC default). The non-atomicity is acceptable here because failures at any step
     * result in containerStatus = "FAILED", giving the candidate a clear error state.
     */
    @Async
    public void initializeContainerAsync(UUID interviewId) {
        try {
            Interview interview = repository.findById(interviewId)
                    .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));

            var question = questionService.getQuestion(interview.getQuestionId());

            String containerId = null;
            if (question.image() != null && !question.image().isBlank()) {
                log.info("[interview={}] Starting container from image: {}", interviewId, question.image());
                // ContainerService.startContainer() delegates to DockerContainerManager,
                // which pulls the image first if not present locally.
                containerId = containerService.startContainer(question.image());
                interview.assignContainer(containerId);
                repository.save(interview);
                log.info("[interview={}] Container started: {}", interviewId, containerId);
            } else {
                log.info("[interview={}] No Docker image configured, skipping container start", interviewId);
            }

            if (containerId != null) {
                try {
                    log.info("[interview={}] Loading exam.yml from container {}", interviewId, containerId);
                    ExamConfig examConfig = examConfigService.getExamConfig(containerId);
                    log.info("[interview={}] exam.yml loaded: {} checkpoint(s)", interviewId, examConfig.checkpoints().size());
                    initializeCheckpointResults(interview, examConfig);
                    log.info("[interview={}] Checkpoint results initialized", interviewId);
                } catch (ExamConfigNotFoundException e) {
                    log.warn("[interview={}] exam.yml not found in container {}. "
                            + "Candidate can still access the workspace.",
                            interviewId, containerId);
                }
            }

            interview.setContainerStatus("READY");
            repository.save(interview);
            log.info("[interview={}] Container initialization complete — status=READY", interviewId);

            eventPublisher.publishEvent(new InterviewStartedEvent(interviewId));

        } catch (Exception e) {
            log.error("Container initialization failed for interview {}: {}", interviewId, e.getMessage(), e);
            // Use a fresh read so we don't accidentally save a stale/incomplete entity.
            repository.findById(interviewId).ifPresent(interview -> {
                interview.setContainerStatus("FAILED");
                repository.save(interview);
            });
        }
    }

    private void initializeCheckpointResults(Interview interview, ExamConfig examConfig) {
        List<ExamConfig.ExamCheckpoint> checkpoints = examConfig.checkpoints();
        List<CheckpointResult> pendingResults = new ArrayList<>();
        for (int i = 0; i < checkpoints.size(); i++) {
            var cp = checkpoints.get(i);
            pendingResults.add(CheckpointResult.createPending(interview.getId(), cp.id(), i + 1));
            log.info("[interview={}] Registering checkpoint [{}/{}] id={} title={}",
                    interview.getId(), i + 1, checkpoints.size(), cp.id(), cp.title());
        }
        checkpointResultRepository.saveAll(pendingResults);
    }
}
