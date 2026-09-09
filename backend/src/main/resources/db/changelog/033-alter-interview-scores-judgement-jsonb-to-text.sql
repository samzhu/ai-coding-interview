-- liquibase formatted sql

-- changeset claude:033
-- 設計說明：pilot_judgement_json 在 Java 側是 String 欄位，Spring Data JDBC 預設
-- 無法把 String 直接寫入 JSONB（需要 PGobject 包裝）。
-- 改為 TEXT 型別：JSON 內容仍然儲存，但不強制做 PostgreSQL JSONB 格式驗證。
-- 這是最小阻力路徑，避免在 JDBC 層添加複雜的自訂 WritingConverter。
ALTER TABLE interview_scores ALTER COLUMN pilot_judgement_json TYPE TEXT USING pilot_judgement_json::TEXT;
-- rollback ALTER TABLE interview_scores ALTER COLUMN pilot_judgement_json TYPE JSONB USING pilot_judgement_json::JSONB;
