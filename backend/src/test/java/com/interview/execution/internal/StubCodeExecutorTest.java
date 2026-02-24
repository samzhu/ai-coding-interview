package com.interview.execution.internal;

import com.interview.execution.CodeExecutionRequest;
import com.interview.execution.ExecutionStatus;
import com.interview.execution.ProjectExecutionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StubCodeExecutor 模擬執行器")
class StubCodeExecutorTest {

    private final StubCodeExecutor executor = new StubCodeExecutor();

    @Test
    @DisplayName("execute 應回傳 SUCCESS 狀態，exitCode 為 0")
    void shouldReturnSuccessForExecute() {
        var request = new CodeExecutionRequest("python", "print('hello')", "", 10);
        var result = executor.execute(request);

        assertThat(result.status()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).isNotBlank();
    }

    @Test
    @DisplayName("executeProject 應回傳 SUCCESS 狀態，exitCode 為 0")
    void shouldReturnSuccessForExecuteProject() {
        var request = new ProjectExecutionRequest(
                Map.of("main.py", "print('hello')"),
                "python main.py",
                30,
                null);
        var result = executor.executeProject(request);

        assertThat(result.status()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).isNotBlank();
    }
}
