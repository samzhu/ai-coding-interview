package com.interview.ai.interfaces.rest;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "message is required")
        String message) {
}
