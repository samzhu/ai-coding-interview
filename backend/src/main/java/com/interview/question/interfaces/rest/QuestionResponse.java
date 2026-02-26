package com.interview.question.interfaces.rest;

import com.interview.question.QuestionDetail;

public record QuestionResponse(
        String id,
        String title,
        String description,
        String difficulty,
        String language,
        String type,
        String level,
        String image) {

    public static QuestionResponse from(QuestionDetail detail) {
        return new QuestionResponse(
                detail.id(),
                detail.title(),
                detail.description(),
                detail.difficulty(),
                detail.language(),
                detail.type(),
                detail.level(),
                detail.image());
    }
}
