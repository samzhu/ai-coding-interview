package com.interview.interview.interfaces.rest;

import com.interview.interview.application.CheckpointProgressService.CheckpointView;

import java.time.Instant;

public record CheckpointResultResponse(
        String checkpointId,
        int sequenceNumber,
        String title,
        String description,
        String starterCode,
        String testCommand,
        String status,
        String submittedCode,
        String executionOutput,
        Instant passedAt) {

    public static CheckpointResultResponse from(CheckpointView view) {
        return new CheckpointResultResponse(
                view.checkpointId(),
                view.sequenceNumber(),
                view.title(),
                view.description(),
                view.starterCode(),
                view.testCommand(),
                view.status(),
                view.submittedCode(),
                view.executionOutput(),
                view.passedAt());
    }
}
