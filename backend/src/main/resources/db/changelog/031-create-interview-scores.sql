--liquibase formatted sql

--changeset claude:031
-- 設計說明：完整 LLM 判斷以 JSONB 存原樣（pilot_judgement_json），不拆欄位 ──
-- schema 可能演進，JSONB 讓未來改 prompt/schema 不用 migration。
-- pilot_headline 與 pilot_summary 額外抽出 column，是因為列表頁與詳情頁
-- 高頻顯示，避免每次解 JSONB。

CREATE TABLE interview_scores (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id          UUID         NOT NULL UNIQUE REFERENCES interviews(id),
    scored_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Outcome 軸（規則計算）
    checkpoints_total     INTEGER      NOT NULL,
    checkpoints_passed    INTEGER      NOT NULL,
    outcome_score         NUMERIC(3,2) NOT NULL,

    -- Agency 軸（Gemini Judge）── 允許 NULL：LLM 失敗時仍寫入 outcome 與 error_reason
    pilot_score           NUMERIC(3,2),
    pilot_verdict         VARCHAR(20),
    pilot_headline        TEXT,
    pilot_summary         TEXT,
    pilot_judgement_json  JSONB,

    -- 重現性 metadata
    judge_model           VARCHAR(50),
    judge_prompt_version  VARCHAR(20),
    timeline_token_count  INTEGER,
    judge_error_reason    TEXT,
    scoring_version       VARCHAR(10)  NOT NULL DEFAULT 'v1'
);

CREATE INDEX idx_scores_interview ON interview_scores(interview_id);
