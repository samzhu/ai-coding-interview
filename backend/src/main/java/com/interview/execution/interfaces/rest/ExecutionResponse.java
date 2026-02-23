package com.interview.execution.interfaces.rest;

import com.interview.execution.CodeExecutionResult;

public record ExecutionResponse(
        int exitCode,
        String stdout,
        String stderr,
        long durationMs,
        String status) {

    public static ExecutionResponse from(CodeExecutionResult result) {
        return new ExecutionResponse(
                result.exitCode(),
                result.stdout(),
                result.stderr(),
                result.durationMs(),
                result.status().name());
    }
}
