package com.interview.question;

import java.util.List;

public record CheckpointDetail(
        String id,
        int sequenceNumber,
        String title,
        String description,
        String starterCode,
        String testCommand,
        List<TestCaseDetail> testCases) {
}
