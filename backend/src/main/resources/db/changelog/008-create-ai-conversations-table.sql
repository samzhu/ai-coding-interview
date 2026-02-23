-- liquibase formatted sql

-- changeset system:008-create-ai-conversations-table
CREATE TABLE ai_conversations (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    interview_id UUID        NOT NULL,
    role         VARCHAR(20) NOT NULL,
    content      TEXT        NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_ai_conversations PRIMARY KEY (id),
    CONSTRAINT fk_ai_conversations_interview FOREIGN KEY (interview_id) REFERENCES interviews(id),
    CONSTRAINT chk_conversation_role CHECK (role IN ('SYSTEM', 'USER', 'ASSISTANT'))
);
-- rollback DROP TABLE ai_conversations;

-- changeset system:008-create-ai-conversations-index
CREATE INDEX idx_ai_conversations_interview_id ON ai_conversations (interview_id, created_at);
-- rollback DROP INDEX idx_ai_conversations_interview_id;
