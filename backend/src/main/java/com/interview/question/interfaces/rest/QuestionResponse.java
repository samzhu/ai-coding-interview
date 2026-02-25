package com.interview.question.interfaces.rest;

import com.interview.question.QuestionDetail;

import java.util.List;

public record QuestionResponse(
        String id,
        String title,
        String description,
        String difficulty,
        String language,
        String type,
        String level,
        String workspace,
        List<CheckpointResponse> checkpoints) {

    public static QuestionResponse from(QuestionDetail detail) {
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
                detail.workspace(),
                checkpoints);
    }
}
