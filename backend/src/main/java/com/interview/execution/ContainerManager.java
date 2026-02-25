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
}
