package com.interview.execution;

public record CodeExecutionRequest(
        String language,
        String code,
        String stdin,
        int timeoutSeconds) {
}
