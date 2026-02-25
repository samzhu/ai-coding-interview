package com.interview.question;

import java.util.List;

public record QuestionDetail(
        String id,
        String title,
        String description,
        String difficulty,
        String language,
        String type,
        String level,
        String image,
        String workspace,
        List<CheckpointDetail> checkpoints) {
}
