--liquibase formatted sql
--changeset batch12:015-alter-interviews-add-timer

ALTER TABLE interviews
    ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 60;
