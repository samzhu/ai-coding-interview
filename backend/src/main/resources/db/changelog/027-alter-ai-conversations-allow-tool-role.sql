-- liquibase formatted sql

-- changeset system:027-alter-ai-conversations-allow-tool-role
-- 設計說明：ToolCallAdvisor 會將 tool calling 中間訊息存入 ChatMemory，
-- 需支援 TOOL 角色。AssistantMessage 帶 toolCalls 時 content 可能為 null。
ALTER TABLE ai_conversations DROP CONSTRAINT chk_conversation_role;
ALTER TABLE ai_conversations ADD CONSTRAINT chk_conversation_role
    CHECK (role IN ('SYSTEM', 'USER', 'ASSISTANT', 'TOOL'));
ALTER TABLE ai_conversations ALTER COLUMN content DROP NOT NULL;
-- rollback ALTER TABLE ai_conversations ALTER COLUMN content SET NOT NULL;
-- rollback ALTER TABLE ai_conversations DROP CONSTRAINT chk_conversation_role;
-- rollback ALTER TABLE ai_conversations ADD CONSTRAINT chk_conversation_role CHECK (role IN ('SYSTEM', 'USER', 'ASSISTANT'));
