package com.interview.execution.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExecuteCodeRequest(
        @NotBlank(message = "language is required")
        String language,

        @NotBlank(message = "code is required")
        String code,

        String stdin,

        @NotNull @Positive
        Integer timeoutSeconds) {
}
