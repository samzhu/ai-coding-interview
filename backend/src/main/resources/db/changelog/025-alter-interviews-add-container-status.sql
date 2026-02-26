--liquibase formatted sql
--changeset ai-interview:025

ALTER TABLE interviews ADD COLUMN container_status VARCHAR(20) DEFAULT NULL;
