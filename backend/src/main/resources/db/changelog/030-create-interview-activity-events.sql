--liquibase formatted sql

--changeset claude:030
-- 設計說明：通用 L1 行為事件儲存。Pilot Score 的 LLM judge 從這張表 + ai_conversations
-- + checkpoint_results 三源 merge 出 timeline。payload 用 JSONB 是因為事件種類會增加
-- (ai.edit.proposal/accepted/rejected/file.opened/file.closed/terminal.test.run/code.manual_edit)
-- 不希望每加一種就 schema migration。

CREATE TABLE interview_activity_events (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID        NOT NULL REFERENCES interviews(id),
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    event_type   VARCHAR(50) NOT NULL,
    payload      JSONB       NOT NULL DEFAULT '{}'::jsonb
);

-- 設計說明：依 interview_id + 時間排序是 timeline 重建的主要查詢模式
CREATE INDEX idx_activity_events_interview_time
    ON interview_activity_events (interview_id, occurred_at);
