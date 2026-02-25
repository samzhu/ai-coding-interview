package com.interview.execution;

import org.springframework.stereotype.Service;

import java.util.List;

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
}
