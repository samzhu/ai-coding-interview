package com.interview.execution;

import org.springframework.stereotype.Service;

@Service
public class CodeExecutionService {

    private final CodeExecutor executor;

    public CodeExecutionService(CodeExecutor executor) {
        this.executor = executor;
    }

    public CodeExecutionResult execute(CodeExecutionRequest request) {
        return executor.execute(request);
    }

    public CodeExecutionResult executeProject(ProjectExecutionRequest request) {
        return executor.executeProject(request);
    }
}
