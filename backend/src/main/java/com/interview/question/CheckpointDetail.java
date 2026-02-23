package com.interview.question;

import java.util.List;
import java.util.UUID;

public record CheckpointDetail(
        UUID id,
        int sequenceNumber,
        String title,
        String description,
        String starterCode,
        String testCommand,
        boolean aiEnabled,
        List<TestCaseDetail> testCases) {
}
