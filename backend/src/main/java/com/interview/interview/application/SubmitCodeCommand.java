package com.interview.interview.application;

import java.util.UUID;

public record SubmitCodeCommand(
        UUID interviewId,
        String checkpointId) {
}
