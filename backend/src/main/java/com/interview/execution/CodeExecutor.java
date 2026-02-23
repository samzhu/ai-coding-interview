package com.interview.execution;

public interface CodeExecutor {

    CodeExecutionResult execute(CodeExecutionRequest request);

    CodeExecutionResult executeProject(ProjectExecutionRequest request);
}
