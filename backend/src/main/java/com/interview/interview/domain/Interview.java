package com.interview.interview.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Table("interviews")
public class Interview {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column("candidate_id")
    private UUID candidateId;

    @Column("interviewer_id")
    private UUID interviewerId;

    private String title;

    private String status;

    @Column("scheduled_at")
    private Instant scheduledAt;

    @Column("started_at")
    private Instant startedAt;

    @Column("completed_at")
    private Instant completedAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("question_id")
    private String questionId;

    @Column("ai_model")
    private String aiModel;

    @Column("duration_minutes")
    private int durationMinutes = 60;

    // Required by Spring Data JDBC
    protected Interview() {
    }

    public static Interview schedule(UUID candidateId, UUID interviewerId, String title, Instant scheduledAt, String questionId, String aiModel) {
        return schedule(candidateId, interviewerId, title, scheduledAt, questionId, aiModel, null);
    }

    public static Interview schedule(UUID candidateId, UUID interviewerId, String title, Instant scheduledAt, String questionId, String aiModel, Integer durationMinutes) {
        Objects.requireNonNull(candidateId, "candidateId must not be null");
        Objects.requireNonNull(interviewerId, "interviewerId must not be null");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be null or blank");
        }
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
        Objects.requireNonNull(questionId, "questionId must not be null");

        Interview interview = new Interview();
        interview.id = UUID.randomUUID();
        interview.candidateId = candidateId;
        interview.interviewerId = interviewerId;
        interview.title = title;
        interview.status = InterviewStatus.SCHEDULED.name();
        interview.scheduledAt = scheduledAt;
        interview.questionId = questionId;
        interview.aiModel = (aiModel != null && !aiModel.isBlank()) ? aiModel : DEFAULT_MODEL;
        interview.durationMinutes = (durationMinutes != null && durationMinutes > 0) ? durationMinutes : 60;
        interview.createdAt = Instant.now();
        interview.updatedAt = Instant.now();
        return interview;
    }

    public void start() {
        if (!getInterviewStatus().canTransitionTo(InterviewStatus.IN_PROGRESS)) {
            throw new IllegalStateException(
                    "Cannot start interview with status " + this.status);
        }
        this.status = InterviewStatus.IN_PROGRESS.name();
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (!getInterviewStatus().canTransitionTo(InterviewStatus.COMPLETED)) {
            throw new IllegalStateException(
                    "Cannot complete interview with status " + this.status);
        }
        this.status = InterviewStatus.COMPLETED.name();
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (!getInterviewStatus().canTransitionTo(InterviewStatus.CANCELLED)) {
            throw new IllegalStateException(
                    "Cannot cancel interview with status " + this.status);
        }
        this.status = InterviewStatus.CANCELLED.name();
        this.updatedAt = Instant.now();
    }

    public InterviewStatus getInterviewStatus() {
        return InterviewStatus.valueOf(this.status);
    }

    public UUID getId() { return id; }
    public UUID getCandidateId() { return candidateId; }
    public UUID getInterviewerId() { return interviewerId; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getQuestionId() { return questionId; }
    public String getAiModel() { return aiModel; }
    public int getDurationMinutes() { return durationMinutes; }
}
