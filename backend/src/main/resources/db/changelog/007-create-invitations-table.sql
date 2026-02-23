-- liquibase formatted sql

-- changeset system:007-create-invitations-table
CREATE TABLE interview_invitations (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    interview_id UUID         NOT NULL UNIQUE,
    token        VARCHAR(64)  NOT NULL UNIQUE,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_interview_invitations PRIMARY KEY (id),
    CONSTRAINT fk_invitations_interview FOREIGN KEY (interview_id) REFERENCES interviews(id)
);
-- rollback DROP TABLE interview_invitations;

-- changeset system:007-create-invitations-index
CREATE INDEX idx_invitations_token ON interview_invitations (token);
-- rollback DROP INDEX idx_invitations_token;
