package com.interview.question;

import java.util.List;
import java.util.UUID;

public record QuestionDetail(
        UUID id,
        String title,
        String description,
        String difficulty,
        String language,
        String type,
        String level,
        List<ProjectFileDetail> projectFiles,
        List<CheckpointDetail> checkpoints) {
}
