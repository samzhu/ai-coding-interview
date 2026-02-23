-- liquibase formatted sql

-- changeset system:006-create-checkpoint-results-table
CREATE TABLE interview_checkpoint_results (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    interview_id        UUID        NOT NULL,
    checkpoint_id       UUID        NOT NULL,
    checkpoint_sequence INT         NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_code      TEXT,
    execution_output    TEXT,
    passed_at           TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version             BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_checkpoint_results PRIMARY KEY (id),
    CONSTRAINT fk_checkpoint_results_interview FOREIGN KEY (interview_id) REFERENCES interviews(id),
    CONSTRAINT uq_checkpoint_result UNIQUE (interview_id, checkpoint_id),
    CONSTRAINT chk_checkpoint_result_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'PASSED', 'FAILED')
    )
);
-- rollback DROP TABLE interview_checkpoint_results;

-- changeset system:006-create-checkpoint-results-indexes
CREATE INDEX idx_checkpoint_results_interview_id ON interview_checkpoint_results (interview_id);
CREATE INDEX idx_checkpoint_results_status ON interview_checkpoint_results (interview_id, status);
-- rollback DROP INDEX idx_checkpoint_results_interview_id; DROP INDEX idx_checkpoint_results_status;
