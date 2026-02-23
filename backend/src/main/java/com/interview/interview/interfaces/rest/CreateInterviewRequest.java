package com.interview.interview.interfaces.rest;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateInterviewRequest(
        @NotNull(message = "candidateId is required")
        UUID candidateId,

        @NotNull(message = "interviewerId is required")
        UUID interviewerId,

        @NotBlank(message = "title is required")
        String title,

        @NotNull(message = "scheduledAt is required")
        Instant scheduledAt,

        @NotNull(message = "questionId is required")
        UUID questionId,

        @Nullable
        String aiModel,

        @Nullable
        Integer durationMinutes) {
}
