package com.interview.question.interfaces.rest;

import com.interview.question.CheckpointDetail;

import java.util.List;

record CheckpointResponse(
        String id,
        int sequenceNumber,
        String title,
        String description,
        String starterCode,
        String testCommand,
        List<TestCaseResponse> testCases) {

    static CheckpointResponse from(CheckpointDetail detail) {
        List<TestCaseResponse> testCases = detail.testCases().stream()
                .filter(tc -> !tc.isHidden())
                .map(TestCaseResponse::from)
                .toList();
        return new CheckpointResponse(
                detail.id(),
                detail.sequenceNumber(),
                detail.title(),
                detail.description(),
                detail.starterCode(),
                detail.testCommand(),
                testCases);
    }
}
