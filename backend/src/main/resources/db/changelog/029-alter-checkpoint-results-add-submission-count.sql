--liquibase formatted sql

--changeset claude:029-alter-checkpoint-results-add-submission-count
-- 設計說明：每次候選人提交 checkpoint 時遞增。
-- Pilot Score 的 Recovery & Verification 維度需要看「重試次數」與「失敗後是否帶新資訊重 prompt」，
-- 不為此建獨立事件表是因為 submission_count 與 status 強耦合在同一 row 上。

ALTER TABLE interview_checkpoint_results
    ADD COLUMN submission_count INTEGER NOT NULL DEFAULT 0;
