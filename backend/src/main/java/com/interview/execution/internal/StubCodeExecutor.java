package com.interview.execution.internal;

import com.interview.execution.CodeExecutionRequest;
import com.interview.execution.CodeExecutionResult;
import com.interview.execution.CodeExecutor;
import com.interview.execution.ExecutionStatus;
import com.interview.execution.ProjectExecutionRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Lab（Cloud Run）環境使用的程式碼執行器 stub。
 * Cloud Run 不支援 Docker daemon，此 stub 回傳模擬成功結果。
 * 未來可替換為 TCP 連線遠端 Docker daemon（GCE VM tcp://host:2376 TLS）。
 */
@Component
@Profile("lab")
class StubCodeExecutor implements CodeExecutor {

    @Override
    public CodeExecutionResult execute(CodeExecutionRequest request) {
        return new CodeExecutionResult(
                0,
                "PASS | stub execution result\n",
                "",
                0L,
                ExecutionStatus.SUCCESS);
    }

    @Override
    public CodeExecutionResult executeProject(ProjectExecutionRequest request) {
        return new CodeExecutionResult(
                0,
                "All tests passed (stub execution)\n",
                "",
                0L,
                ExecutionStatus.SUCCESS);
    }
}
