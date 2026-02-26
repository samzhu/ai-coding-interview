package com.interview.question.application;

/**
 * YAML-mapped definition of a question loaded from classpath:questions/*.yaml.
 * Runtime fields (workspace, exclude, checkpoints) come from the container's exam.yml.
 */
public record QuestionDefinition(
        String id,
        String title,
        String description,
        String difficulty,
        String language,
        String type,
        String level,
        String image) {
}
