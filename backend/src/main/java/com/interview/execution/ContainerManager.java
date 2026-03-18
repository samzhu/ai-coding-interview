package com.interview.execution;

import java.util.List;
import java.util.function.Consumer;

public interface ContainerManager {

    String startContainer(String image, int memoryMb, int cpuCount);

    void stopContainer(String containerId);

    boolean isRunning(String containerId);

    /** Single-level listing of a directory. Returns files and immediate subdirectories. */
    List<ContainerFile> listDirectory(String containerId, String dir);

    /** Recursive listing up to maxDepth. Returns files and directories with correct isDirectory flag. */
    List<ContainerFile> directoryTree(String containerId, String dir, int maxDepth);

    String readFile(String containerId, String path);

    void writeFile(String containerId, String path, String content);

    CodeExecutionResult execCommand(String containerId, String command, int timeoutSeconds);

    /**
     * Executes a command in the container and calls outputConsumer for each output frame received.
     * Enables real-time streaming of console output to the caller.
     * Default implementation ignores the consumer and delegates to the 3-param version.
     */
    default CodeExecutionResult execCommand(String containerId, String command,
                                             int timeoutSeconds, Consumer<String> outputConsumer) {
        return execCommand(containerId, command, timeoutSeconds);
    }

    /**
     * 在指定容器中建立互動式 bash 終端 session（TTY 模式）。
     *
     * @param containerId 目標容器 ID
     * @param workDir     bash 的初始工作目錄（通常為 exam.yml 的 workspace 路徑）
     * @return 新建的 TerminalSession，呼叫端負責在連線關閉時呼叫 close()
     */
    TerminalSession createTerminalSession(String containerId, String workDir);
}
