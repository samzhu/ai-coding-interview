package com.interview.interview.application;

import com.interview.interview.InterviewStartedEvent;
import com.interview.interview.domain.CheckpointResult;
import com.interview.interview.domain.Interview;
import com.interview.interview.InterviewCompletedEvent;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import com.interview.interview.interfaces.rest.TimeRemainingResponse;
import com.interview.question.QuestionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InterviewService {

    private final InterviewRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final CheckpointResultRepository checkpointResultRepository;
    private final QuestionService questionService;

    public InterviewService(InterviewRepository repository,
                            ApplicationEventPublisher eventPublisher,
                            CheckpointResultRepository checkpointResultRepository,
                            QuestionService questionService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.checkpointResultRepository = checkpointResultRepository;
        this.questionService = questionService;
    }

    public Interview createInterview(CreateInterviewCommand command) {
        Interview interview = Interview.schedule(
                command.candidateId(),
                command.interviewerId(),
                command.title(),
                command.scheduledAt(),
                command.questionId(),
                command.aiModel(),
                command.durationMinutes());
        return repository.save(interview);
    }

    public Interview startInterview(UUID interviewId) {
        Interview interview = repository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        interview.start();
        Interview saved = repository.save(interview);

        initializeCheckpointResults(saved);
        eventPublisher.publishEvent(new InterviewStartedEvent(interviewId));

        return saved;
    }

    public Interview completeInterview(UUID interviewId) {
        Interview interview = repository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        interview.complete();
        Interview saved = repository.save(interview);
        eventPublisher.publishEvent(new InterviewCompletedEvent(interviewId));
        return saved;
    }

    public Interview cancelInterview(UUID interviewId) {
        Interview interview = repository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        interview.cancel();
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

    private void initializeCheckpointResults(Interview interview) {
        var question = questionService.getQuestion(interview.getQuestionId());
        List<CheckpointResult> pendingResults = question.checkpoints().stream()
                .map(cp -> CheckpointResult.createPending(
                        interview.getId(), cp.id(), cp.sequenceNumber()))
                .toList();
        checkpointResultRepository.saveAll(pendingResults);
    }
}
