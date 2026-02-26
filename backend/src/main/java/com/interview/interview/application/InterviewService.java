package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.execution.ExamConfigService;
import com.interview.interview.domain.Interview;
import com.interview.interview.InterviewCompletedEvent;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import com.interview.interview.interfaces.rest.TimeRemainingResponse;
import com.interview.question.QuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final InterviewRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final QuestionService questionService;
    private final ContainerService containerService;
    private final ExamConfigService examConfigService;
    private final ContainerInitializationService containerInitService;

    public InterviewService(InterviewRepository repository,
                            ApplicationEventPublisher eventPublisher,
                            QuestionService questionService,
                            ContainerService containerService,
                            ExamConfigService examConfigService,
                            ContainerInitializationService containerInitService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.questionService = questionService;
        this.containerService = containerService;
        this.examConfigService = examConfigService;
        this.containerInitService = containerInitService;
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

        // Only mark INITIALIZING when a Docker image needs to be pulled/started.
        // Questions without a container image skip async init entirely.
        var question = questionService.getQuestion(interview.getQuestionId());
        boolean needsContainer = question.image() != null && !question.image().isBlank();
        if (needsContainer) {
            interview.setContainerStatus("INITIALIZING");
        }

        Interview saved = repository.save(interview);

        if (needsContainer) {
            // Trigger async container init AFTER this transaction commits so the async
            // thread always reads the committed "INITIALIZING" row and never races with us.
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        containerInitService.initializeContainerAsync(interviewId);
                    }
                });
            } else {
                // No active transaction (e.g. in unit tests) — call directly.
                containerInitService.initializeContainerAsync(interviewId);
            }
        } else {
            // No container needed: publish started event synchronously.
            eventPublisher.publishEvent(new com.interview.interview.InterviewStartedEvent(interviewId));
        }

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
     * Throws ContainerNotReadyException if the container is still being initialised
     * so the frontend can keep polling /container-status instead of getting a confusing error.
     * If containerId is blank or the container has stopped, starts a new one and persists it.
     * Returns the containerId.
     */
    public String ensureContainerRunning(UUID interviewId) {
        Interview interview = repository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));

        // Container is being pulled/started asynchronously — reject immediately.
        if ("INITIALIZING".equals(interview.getContainerStatus())) {
            throw new ContainerNotReadyException(
                    "Container is still being initialized for interview: " + interviewId);
        }

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

}

