-- liquibase formatted sql

-- changeset system:023-alter-question-id-to-varchar
-- question_id: UUID → VARCHAR
ALTER TABLE interviews ALTER COLUMN question_id TYPE VARCHAR(255) USING question_id::text;
-- rollback ALTER TABLE interviews ALTER COLUMN question_id TYPE UUID USING question_id::uuid;

-- changeset system:023-alter-checkpoint-id-to-varchar
-- checkpoint_id: UUID → VARCHAR
ALTER TABLE interview_checkpoint_results ALTER COLUMN checkpoint_id TYPE VARCHAR(255) USING checkpoint_id::text;
-- rollback ALTER TABLE interview_checkpoint_results ALTER COLUMN checkpoint_id TYPE UUID USING checkpoint_id::uuid;
