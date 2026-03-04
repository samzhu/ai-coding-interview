package com.interview.execution;

import java.util.List;

public interface ContainerManager {

    String startContainer(String image, int memoryMb, int cpuCount);

    void stopContainer(String containerId);

    boolean isRunning(String containerId);

    List<ContainerFile> listFiles(String containerId, String dir);

    String readFile(String containerId, String path);

    void writeFile(String containerId, String path, String content);

    CodeExecutionResult execCommand(String containerId, String command, int timeoutSeconds);

    /**
     * 在指定容器中建立互動式 bash 終端 session（TTY 模式）。
     *
     * @param containerId 目標容器 ID
     * @param workDir     bash 的初始工作目錄（通常為 exam.yml 的 workspace 路徑）
     * @return 新建的 TerminalSession，呼叫端負責在連線關閉時呼叫 close()
     */
    TerminalSession createTerminalSession(String containerId, String workDir);
}
