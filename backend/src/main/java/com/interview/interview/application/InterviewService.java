package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.execution.ExamConfig;
import com.interview.execution.ExamConfigNotFoundException;
import com.interview.execution.ExamConfigService;
import com.interview.interview.InterviewStartedEvent;
import com.interview.interview.domain.CheckpointResult;
import com.interview.interview.domain.Interview;
import com.interview.interview.InterviewCompletedEvent;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import com.interview.interview.interfaces.rest.TimeRemainingResponse;
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
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final InterviewRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final CheckpointResultRepository checkpointResultRepository;
    private final QuestionService questionService;
    private final ContainerService containerService;
    private final ExamConfigService examConfigService;

    public InterviewService(InterviewRepository repository,
                            ApplicationEventPublisher eventPublisher,
                            CheckpointResultRepository checkpointResultRepository,
                            QuestionService questionService,
                            ContainerService containerService,
                            ExamConfigService examConfigService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.checkpointResultRepository = checkpointResultRepository;
        this.questionService = questionService;
        this.containerService = containerService;
        this.examConfigService = examConfigService;
    }

    public Interview createInterview(CreateInterviewCommand command) {
        Interview interview = Interview.schedule(
                command.candidateId(),
                command.interviewerId(),
                command.title(),
                command.scheduledAt(),
                command.questionId(),
                null,
                command.durationMinutes());
        return repository.save(interview);
    }

    public Interview startInterview(UUID interviewId) {
        Interview interview = repository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        interview.start();

        // Start a persistent container for this interview session
        var question = questionService.getQuestion(interview.getQuestionId());
        String containerId = null;
        if (question.image() != null && !question.image().isBlank()) {
            try {
                containerId = containerService.startContainer(question.image());
                interview.assignContainer(containerId);
                log.info("Started container {} for interview {}", containerId, interviewId);
            } catch (Exception e) {
                log.warn("Failed to start container for interview {}: {}", interviewId, e.getMessage());
            }
        }

        Interview saved = repository.save(interview);

        // Read exam.yml from container and initialize checkpoints.
        // If exam.yml is missing, log a warning and skip checkpoint initialization —
        // the interview still starts so the candidate can access the workspace.
        if (containerId != null) {
            try {
                ExamConfig examConfig = examConfigService.getExamConfig(containerId);
                initializeCheckpointResults(saved, examConfig);
            } catch (ExamConfigNotFoundException e) {
                log.warn("exam.yml not found in container {} for interview {}. "
                        + "Candidate can still access the workspace. "
                        + "Interviewer should verify exam content.",
                        containerId, interviewId);
            }
        }

        eventPublisher.publishEvent(new InterviewStartedEvent(interviewId));
        return saved;
    }

    public Interview completeInterview(UUID interviewId) {
        Interview interview = repository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        stopContainerIfRunning(interview);
        interview.complete();
        interview.clearContainer();
        Interview saved = repository.save(interview);
        eventPublisher.publishEvent(new InterviewCompletedEvent(interviewId));
        return saved;
    }

    public Interview cancelInterview(UUID interviewId) {
        Interview interview = repository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        stopContainerIfRunning(interview);
        interview.cancel();
        interview.clearContainer();
        return repository.save(interview);
    }

    @Transactional(readOnly = true)
    public Interview findById(UUID interviewId) {
        return repository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
    }

    @Transactional(readOnly = true)
    public List<Interview> findAll() {
        return repository.findAll().stream().toList();
    }

    @Transactional(readOnly = true)
    public TimeRemainingResponse getTimeRemaining(UUID interviewId) {
        Interview interview = repository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));

        long totalSeconds = (long) interview.getDurationMinutes() * 60;

        if (interview.getStartedAt() == null) {
            return new TimeRemainingResponse(totalSeconds, totalSeconds);
        }

        long elapsedSeconds = Instant.now().getEpochSecond() - interview.getStartedAt().getEpochSecond();
        long remainingSeconds = Math.max(0, totalSeconds - elapsedSeconds);
        return new TimeRemainingResponse(remainingSeconds, totalSeconds);
    }

    /**
     * Ensures the interview has a running container.
     * If containerId is blank or the container has stopped, starts a new one and persists it.
     * Returns the containerId.
     */
    public String ensureContainerRunning(UUID interviewId) {
        Interview interview = repository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));

        String containerId = interview.getContainerId();
        if (containerId != null && !containerId.isBlank()) {
            if (containerService.isRunning(containerId)) {
                return containerId;
            }
            // Old container is gone — evict its cached exam config
            examConfigService.evict(containerId);
            log.warn("Container {} is no longer running for interview {}, starting new one", containerId, interviewId);
        }

        var question = questionService.getQuestion(interview.getQuestionId());
        if (question.image() == null || question.image().isBlank()) {
            throw new IllegalStateException("No Docker image configured for question: " + interview.getQuestionId());
        }
        String newContainerId = containerService.startContainer(question.image());
        interview.assignContainer(newContainerId);
        repository.save(interview);
        log.info("On-demand started container {} for interview {}", newContainerId, interviewId);
        return newContainerId;
    }

    private void stopContainerIfRunning(Interview interview) {
        String containerId = interview.getContainerId();
        if (containerId != null && !containerId.isBlank()) {
            try {
                containerService.stopContainer(containerId);
                log.info("Stopped container {} for interview {}", containerId, interview.getId());
            } catch (Exception e) {
                log.warn("Failed to stop container {} for interview {}: {}", containerId, interview.getId(), e.getMessage());
            }
            // 容器停止後清除 exam config 快取
            examConfigService.evict(containerId);
        }
    }

    private void initializeCheckpointResults(Interview interview, ExamConfig examConfig) {
        List<ExamConfig.ExamCheckpoint> checkpoints = examConfig.checkpoints();
        List<CheckpointResult> pendingResults = new java.util.ArrayList<>();
        for (int i = 0; i < checkpoints.size(); i++) {
            pendingResults.add(CheckpointResult.createPending(interview.getId(), checkpoints.get(i).id(), i + 1));
        }
        checkpointResultRepository.saveAll(pendingResults);
    }
}
