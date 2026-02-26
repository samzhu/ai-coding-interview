--liquibase formatted sql

--changeset sam:026
ALTER TABLE ai_conversations ADD COLUMN model VARCHAR(100) DEFAULT NULL;
