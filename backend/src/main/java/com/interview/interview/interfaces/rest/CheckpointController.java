package com.interview.interview.interfaces.rest;

import com.interview.interview.application.CheckpointProgressService;
import com.interview.interview.application.SubmitCodeCommand;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/checkpoints")
class CheckpointController {

    private final CheckpointProgressService progressService;

    CheckpointController(CheckpointProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/current")
    CheckpointResultResponse getCurrentCheckpoint(@PathVariable UUID interviewId) {
        return CheckpointResultResponse.from(progressService.getCurrentCheckpoint(interviewId));
    }

    @PostMapping("/{checkpointId}/submit")
    CheckpointResultResponse submitCode(@PathVariable UUID interviewId,
                                        @PathVariable UUID checkpointId,
                                        @RequestBody SubmitCodeRequest request) {
        SubmitCodeCommand command;
        if (request.files() != null && !request.files().isEmpty()) {
            command = SubmitCodeCommand.multiFile(interviewId, checkpointId, request.files());
        } else {
            command = SubmitCodeCommand.singleFile(interviewId, checkpointId,
                    request.code() != null ? request.code() : "");
        }
        return CheckpointResultResponse.from(progressService.submitCode(command));
    }

    @PostMapping("/{checkpointId}/run")
    CheckpointResultResponse runTests(@PathVariable UUID interviewId,
                                      @PathVariable UUID checkpointId,
                                      @RequestBody(required = false) RunTestsRequest request) {
        Map<String, String> files = request != null ? request.files() : null;
        return CheckpointResultResponse.from(progressService.runTests(interviewId, checkpointId, files));
    }
}
