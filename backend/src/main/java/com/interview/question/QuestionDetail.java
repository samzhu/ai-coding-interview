package com.interview.question;

public record QuestionDetail(
        String id,
        String title,
        String description,
        String difficulty,
        String language,
        String type,
        String level,
        String image) {
}
