-- liquibase formatted sql

-- changeset system:001-create-interviews-table
CREATE TABLE interviews (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    candidate_id    UUID         NOT NULL,
    interviewer_id  UUID         NOT NULL,
    title           VARCHAR(255) NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'SCHEDULED',
    version         BIGINT       NOT NULL DEFAULT 0,
    scheduled_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_interviews PRIMARY KEY (id),
    CONSTRAINT chk_interview_status CHECK (
        status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    )
);
-- rollback DROP TABLE interviews;

-- changeset system:002-create-interview-indexes
CREATE INDEX idx_interviews_candidate_id ON interviews (candidate_id);
CREATE INDEX idx_interviews_status ON interviews (status);
-- rollback DROP INDEX idx_interviews_candidate_id; DROP INDEX idx_interviews_status;
