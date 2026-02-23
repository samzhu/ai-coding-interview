-- liquibase formatted sql

-- changeset system:022-alter-ai-conversations-add-version
ALTER TABLE ai_conversations ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT NULL;
-- rollback ALTER TABLE ai_conversations DROP COLUMN IF EXISTS version;
