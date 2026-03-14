--liquibase formatted sql

--changeset sam:028
-- 設計說明：記錄每則 ASSISTANT 訊息的 token 消耗量，供面試官在 Admin 查看 AI 使用成本。
-- total_tokens 不另存，查詢時以 prompt_tokens + completion_tokens 計算即可。
ALTER TABLE ai_conversations ADD COLUMN prompt_tokens INTEGER DEFAULT NULL;
ALTER TABLE ai_conversations ADD COLUMN completion_tokens INTEGER DEFAULT NULL;
