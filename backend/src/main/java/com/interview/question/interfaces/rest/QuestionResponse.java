package com.interview.question.interfaces.rest;

import com.interview.question.QuestionDetail;

import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        String title,
        String description,
        String difficulty,
        String language,
        String type,
        String level,
        List<ProjectFileResponse> projectFiles,
        List<CheckpointResponse> checkpoints) {

    public static QuestionResponse from(QuestionDetail detail) {
        List<ProjectFileResponse> projectFiles = detail.projectFiles().stream()
                .map(ProjectFileResponse::from)
                .toList();
        List<CheckpointResponse> checkpoints = detail.checkpoints().stream()
                .map(CheckpointResponse::from)
                .toList();
        return new QuestionResponse(
                detail.id(),
                detail.title(),
                detail.description(),
                detail.difficulty(),
                detail.language(),
                detail.type(),
                detail.level(),
                projectFiles,
                checkpoints);
    }
}
