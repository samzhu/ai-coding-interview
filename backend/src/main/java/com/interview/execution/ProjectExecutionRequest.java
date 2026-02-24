package com.interview.execution;

import java.util.Map;

public record ProjectExecutionRequest(
        Map<String, String> files,
        String testCommand,
        int timeoutSeconds,
        String dockerImage) {
}
