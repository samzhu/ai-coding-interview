package com.interview.execution.internal;

import com.interview.execution.CodeExecutionResult;
import com.interview.execution.ContainerFile;
import com.interview.execution.ContainerManager;
import com.interview.execution.ExecutionStatus;
import com.interview.execution.TerminalSession;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component
@Profile("lab")
class StubContainerManager implements ContainerManager {

    @Override
    public String startContainer(String image, int memoryMb, int cpuCount) {
        return "stub-container-" + System.currentTimeMillis();
    }

    @Override
    public void stopContainer(String containerId) {
        // no-op
    }

    @Override
    public boolean isRunning(String containerId) {
        return containerId != null && containerId.startsWith("stub-container-");
    }

    @Override
    public List<ContainerFile> listDirectory(String containerId, String dir) {
        return List.of();
    }

    @Override
    public List<ContainerFile> directoryTree(String containerId, String dir, int maxDepth) {
        return List.of();
    }

    @Override
    public String readFile(String containerId, String path) {
        return "";
    }

    @Override
    public void writeFile(String containerId, String path, String content) {
        // no-op
    }

    @Override
    public CodeExecutionResult execCommand(String containerId, String command, int timeoutSeconds) {
        return new CodeExecutionResult(0, "stub output", "", 0L, ExecutionStatus.SUCCESS);
    }

    @Override
    public CodeExecutionResult execCommand(String containerId, String command,
                                            int timeoutSeconds, Consumer<String> outputConsumer) {
        outputConsumer.accept("stub output\n");
        return new CodeExecutionResult(0, "stub output", "", 0L, ExecutionStatus.SUCCESS);
    }

    @Override
    public TerminalSession createTerminalSession(String containerId, String workDir) {
        // lab profile 下回傳 echo-back stub，不需要真正的 Docker exec
        return new StubTerminalSession();
    }
}
