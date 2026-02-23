-- liquibase formatted sql

-- changeset system:005-add-question-id-to-interviews
-- 新增 question_id 欄位，預設使用 Two Sum 題目（seed UUID）以相容現有測試資料
ALTER TABLE interviews ADD COLUMN question_id UUID NOT NULL DEFAULT '11111111-1111-1111-1111-111111111111';
-- rollback ALTER TABLE interviews DROP COLUMN question_id;

-- changeset system:005-drop-question-id-default
ALTER TABLE interviews ALTER COLUMN question_id DROP DEFAULT;
-- rollback ALTER TABLE interviews ALTER COLUMN question_id SET DEFAULT '11111111-1111-1111-1111-111111111111';

-- changeset system:005-create-interview-question-index
CREATE INDEX idx_interviews_question_id ON interviews (question_id);
-- rollback DROP INDEX idx_interviews_question_id;
