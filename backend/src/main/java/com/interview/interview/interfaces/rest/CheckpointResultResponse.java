package com.interview.interview.interfaces.rest;

import com.interview.interview.application.CheckpointProgressService.CheckpointView;
import com.interview.interview.application.CheckpointProgressService.CheckpointView.ProjectFileView;

import java.time.Instant;
import java.util.List;

public record CheckpointResultResponse(
        String checkpointId,
        int sequenceNumber,
        String title,
        String description,
        String starterCode,
        String testCommand,
        List<ProjectFileResponse> projectFiles,
        String status,
        String submittedCode,
        String executionOutput,
        Instant passedAt) {

    public record ProjectFileResponse(String filePath, String content, boolean editable) {
        static ProjectFileResponse from(ProjectFileView view) {
            return new ProjectFileResponse(view.filePath(), view.content(), view.editable());
        }
    }

    public static CheckpointResultResponse from(CheckpointView view) {
        List<ProjectFileResponse> projectFiles = view.projectFiles().stream()
                .map(ProjectFileResponse::from)
                .toList();
        return new CheckpointResultResponse(
                view.checkpointId(),
                view.sequenceNumber(),
                view.title(),
                view.description(),
                view.starterCode(),
                view.testCommand(),
                projectFiles,
                view.status(),
                view.submittedCode(),
                view.executionOutput(),
                view.passedAt());
    }
}
