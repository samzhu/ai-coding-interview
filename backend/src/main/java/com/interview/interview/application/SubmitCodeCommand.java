package com.interview.interview.application;

import java.util.Map;
import java.util.UUID;

public record SubmitCodeCommand(
        UUID interviewId,
        String checkpointId,
        String code,
        Map<String, String> files) {

    public static SubmitCodeCommand singleFile(UUID interviewId, String checkpointId, String code) {
        return new SubmitCodeCommand(interviewId, checkpointId, code, null);
    }

    public static SubmitCodeCommand multiFile(UUID interviewId, String checkpointId, Map<String, String> files) {
        return new SubmitCodeCommand(interviewId, checkpointId, null, files);
    }
}
