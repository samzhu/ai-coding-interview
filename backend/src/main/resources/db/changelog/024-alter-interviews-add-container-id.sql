--liquibase formatted sql
--changeset ai-interview:024

ALTER TABLE interviews ADD COLUMN container_id VARCHAR(255);
