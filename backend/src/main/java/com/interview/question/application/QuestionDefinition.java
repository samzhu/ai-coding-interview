package com.interview.question.application;

import java.util.List;

/**
 * YAML-mapped definition of a question loaded from classpath:questions/{id}/question.yml.
 */
public record QuestionDefinition(
        String id,
        String title,
        String description,
        String difficulty,
        String language,
        String type,
        String level,
        List<ProjectFileDef> projectFiles,
        List<CheckpointDef> checkpoints) {

    public record ProjectFileDef(
            String filePath,
            boolean editable,
            int sortOrder) {
    }

    public record CheckpointDef(
            String id,
            int sequenceNumber,
            String title,
            String description,
            String testCommand,
            boolean aiEnabled) {
    }
}
