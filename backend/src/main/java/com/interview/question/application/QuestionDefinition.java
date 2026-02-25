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
        String image,
        String workspace,
        List<CheckpointDef> checkpoints,
        List<String> exclude) {

    public record CheckpointDef(
            String id,
            String title,
            String description,
            String testCommand) {
    }
}
