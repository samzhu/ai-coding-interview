package com.interview.interview.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("interview_checkpoint_results")
public class CheckpointResult {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column("interview_id")
    private UUID interviewId;

    @Column("checkpoint_id")
    private UUID checkpointId;

    @Column("checkpoint_sequence")
    private int checkpointSequence;

    private String status;

    @Column("submitted_code")
    private String submittedCode;

    @Column("execution_output")
    private String executionOutput;

    @Column("passed_at")
    private Instant passedAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    protected CheckpointResult() {
    }

    public static CheckpointResult createPending(UUID interviewId, UUID checkpointId, int checkpointSequence) {
        CheckpointResult result = new CheckpointResult();
        result.id = UUID.randomUUID();
        result.interviewId = interviewId;
        result.checkpointId = checkpointId;
        result.checkpointSequence = checkpointSequence;
        result.status = CheckpointResultStatus.PENDING.name();
        result.createdAt = Instant.now();
        result.updatedAt = Instant.now();
        return result;
    }

    public void startProgress(String submittedCode) {
        if (!getCheckpointResultStatus().canTransitionTo(CheckpointResultStatus.IN_PROGRESS)) {
            throw new IllegalStateException("Cannot start progress from status: " + this.status);
        }
        this.status = CheckpointResultStatus.IN_PROGRESS.name();
        this.submittedCode = submittedCode;
        this.updatedAt = Instant.now();
    }

    public void markPassed(String executionOutput) {
        if (!getCheckpointResultStatus().canTransitionTo(CheckpointResultStatus.PASSED)) {
            throw new IllegalStateException("Cannot mark as passed from status: " + this.status);
        }
        this.status = CheckpointResultStatus.PASSED.name();
        this.executionOutput = executionOutput;
        this.passedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markFailed(String executionOutput) {
        if (!getCheckpointResultStatus().canTransitionTo(CheckpointResultStatus.FAILED)) {
            throw new IllegalStateException("Cannot mark as failed from status: " + this.status);
        }
        this.status = CheckpointResultStatus.FAILED.name();
        this.executionOutput = executionOutput;
        this.updatedAt = Instant.now();
    }

    public CheckpointResultStatus getCheckpointResultStatus() {
        return CheckpointResultStatus.valueOf(this.status);
    }

    public UUID getId() { return id; }
    public UUID getInterviewId() { return interviewId; }
    public UUID getCheckpointId() { return checkpointId; }
    public int getCheckpointSequence() { return checkpointSequence; }
    public String getStatus() { return status; }
    public String getSubmittedCode() { return submittedCode; }
    public String getExecutionOutput() { return executionOutput; }
    public Instant getPassedAt() { return passedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
