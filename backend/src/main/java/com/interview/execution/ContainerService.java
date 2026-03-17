package com.interview.execution;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class ContainerService {

    private final ContainerManager containerManager;

    public ContainerService(ContainerManager containerManager) {
        this.containerManager = containerManager;
    }

    public String startContainer(String image) {
        return containerManager.startContainer(image, 256, 1);
    }

    public void stopContainer(String containerId) {
        if (containerId == null || containerId.isBlank()) return;
        containerManager.stopContainer(containerId);
    }

    public boolean isRunning(String containerId) {
        if (containerId == null || containerId.isBlank()) return false;
        return containerManager.isRunning(containerId);
    }

    public List<ContainerFile> listFiles(String containerId, String dir) {
        return containerManager.listFiles(containerId, dir);
    }

    public String readFile(String containerId, String path) {
        return containerManager.readFile(containerId, path);
    }

    public void writeFile(String containerId, String path, String content) {
        containerManager.writeFile(containerId, path, content);
    }

    public CodeExecutionResult execCommand(String containerId, String command, int timeoutSeconds) {
        return containerManager.execCommand(containerId, command, timeoutSeconds);
    }

    /**
     * Streaming variant: calls outputConsumer for each output frame as it arrives.
     * Enables real-time console output during long-running test executions.
     */
    public CodeExecutionResult execCommand(String containerId, String command,
                                            int timeoutSeconds, Consumer<String> outputConsumer) {
        return containerManager.execCommand(containerId, command, timeoutSeconds, outputConsumer);
    }

    /**
     * 在指定容器中建立互動式 bash 終端 session。
     * facade 方法：interview 模組不需要直接依賴 ContainerManager 實作細節。
     */
    public TerminalSession createTerminalSession(String containerId, String workDir) {
        return containerManager.createTerminalSession(containerId, workDir);
    }
}
