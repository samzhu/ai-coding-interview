package com.interview.interview.interfaces.rest;

import com.interview.interview.application.CheckpointProgressService;
import com.interview.interview.application.SubmitCodeCommand;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/checkpoints")
class CheckpointController {

    private final CheckpointProgressService progressService;

    CheckpointController(CheckpointProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    List<CheckpointResultResponse> listCheckpoints(@PathVariable UUID interviewId) {
        return progressService.getAllCheckpoints(interviewId).stream()
                .map(CheckpointResultResponse::from)
                .toList();
    }

    @GetMapping("/current")
    CheckpointResultResponse getCurrentCheckpoint(@PathVariable UUID interviewId) {
        return CheckpointResultResponse.from(progressService.getCurrentCheckpoint(interviewId));
    }

    @PostMapping("/{checkpointId}/submit")
    CheckpointResultResponse submitCode(@PathVariable UUID interviewId,
                                        @PathVariable String checkpointId) {
        return CheckpointResultResponse.from(
                progressService.submitCode(new SubmitCodeCommand(interviewId, checkpointId)));
    }

    @PostMapping("/{checkpointId}/run")
    CheckpointResultResponse runTests(@PathVariable UUID interviewId,
                                      @PathVariable String checkpointId) {
        return CheckpointResultResponse.from(progressService.runTests(interviewId, checkpointId));
    }
}
