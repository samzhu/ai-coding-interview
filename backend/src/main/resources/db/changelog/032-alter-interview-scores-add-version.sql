-- liquibase formatted sql

-- changeset claude:032
-- 設計說明：Spring Data JDBC 的 @Version 欄位用於樂觀鎖定（Optimistic Locking），
-- 確保 ScoringOrchestrator 在並發情況下不會覆蓋彼此的寫入。
-- 031 建立 interview_scores 時遺漏此欄位，補上。
ALTER TABLE interview_scores ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
-- rollback ALTER TABLE interview_scores DROP COLUMN version;
