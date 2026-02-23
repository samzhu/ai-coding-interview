--liquibase formatted sql
--changeset ai-interview:021
ALTER TABLE interviews ALTER COLUMN ai_model SET DEFAULT 'gemini-2.5-flash';
UPDATE interviews SET ai_model = 'gemini-2.5-flash' WHERE ai_model = 'gemini-3-flash-preview';
