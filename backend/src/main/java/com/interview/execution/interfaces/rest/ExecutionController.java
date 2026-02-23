package com.interview.execution.interfaces.rest;

import com.interview.execution.CodeExecutionRequest;
import com.interview.execution.CodeExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/executions")
class ExecutionController {

    private final CodeExecutionService service;

    ExecutionController(CodeExecutionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    ExecutionResponse executeCode(@RequestBody @Valid ExecuteCodeRequest request) {
        var execRequest = new CodeExecutionRequest(
                request.language(),
                request.code(),
                request.stdin(),
                request.timeoutSeconds());
        return ExecutionResponse.from(service.execute(execRequest));
    }
}
