package com.interview.interview.interfaces.rest;

import com.interview.interview.application.TestExecutionProcess;

import java.util.UUID;

/**
 * Response DTO for GET /executions/{processId}.
 * Returned each polling cycle; consoleOutput is the full accumulated output so far.
 */
public record ExecutionStatusResponse(
        UUID processId,
        String status,        // RUNNING, COMPLETED, FAILED
        String consoleOutput, // accumulated output so far (may be partial while RUNNING)
        Integer exitCode,     // null while RUNNING
        Boolean passed        // null while RUNNING
) {
    static ExecutionStatusResponse from(TestExecutionProcess p) {
        return new ExecutionStatusResponse(
                p.getProcessId(),
                p.getStatus(),
                p.getConsoleOutput(),
                p.getExitCode(),
                p.getPassed());
    }
}
