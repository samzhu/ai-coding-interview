package com.interview.interview.bdd;

import com.interview.execution.CodeExecutionRequest;
import com.interview.execution.CodeExecutionResult;
import com.interview.execution.CodeExecutor;
import com.interview.execution.ExecutionStatus;
import com.interview.execution.ProjectExecutionRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestCodeExecutorConfig {

    @Bean
    @Primary
    CodeExecutor controllableCodeExecutor(
            @Qualifier("dockerCodeExecutor") CodeExecutor realExecutor) {
        return new ControllableCodeExecutor(realExecutor);
    }

    static class ControllableCodeExecutor implements CodeExecutor {

        enum Mode { PASS_THROUGH, FORCE_PASS, FORCE_FAIL }

        static volatile Mode currentMode = Mode.PASS_THROUGH;

        private final CodeExecutor delegate;

        ControllableCodeExecutor(CodeExecutor delegate) {
            this.delegate = delegate;
        }

        static void reset()     { currentMode = Mode.PASS_THROUGH; }
        static void forcePass() { currentMode = Mode.FORCE_PASS; }
        static void forceFail() { currentMode = Mode.FORCE_FAIL; }

        @Override
        public CodeExecutionResult execute(CodeExecutionRequest request) {
            return switch (currentMode) {
                case PASS_THROUGH -> delegate.execute(request);
                case FORCE_PASS   -> successResult();
                case FORCE_FAIL   -> failResult();
            };
        }

        @Override
        public CodeExecutionResult executeProject(ProjectExecutionRequest request) {
            return switch (currentMode) {
                case PASS_THROUGH -> delegate.executeProject(request);
                case FORCE_PASS   -> successResult();
                case FORCE_FAIL   -> failResult();
            };
        }

        private static CodeExecutionResult successResult() {
            return new CodeExecutionResult(0, "All tests passed", "", 10L, ExecutionStatus.SUCCESS);
        }

        private static CodeExecutionResult failResult() {
            return new CodeExecutionResult(1, "", "Tests failed", 10L, ExecutionStatus.SUCCESS);
        }
    }
}
