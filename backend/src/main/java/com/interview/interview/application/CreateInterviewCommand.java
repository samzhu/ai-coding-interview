package com.interview.interview.application;

import java.time.Instant;
import java.util.UUID;

public record CreateInterviewCommand(
        UUID candidateId,
        UUID interviewerId,
        String title,
        Instant scheduledAt,
        UUID questionId,
        String aiModel,
        Integer durationMinutes) {
}
