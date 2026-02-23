package com.interview.execution;

public record CodeExecutionResult(
        int exitCode,
        String stdout,
        String stderr,
        long durationMs,
        ExecutionStatus status) {
}
