package com.interview.execution;

/**
 * 容器內找不到 exam.yml 時拋出。
 * 面試官必須在映像檔內提供 /workspace/exam.yml。
 */
public class ExamConfigNotFoundException extends RuntimeException {

    public ExamConfigNotFoundException(String containerId, String path) {
        super("exam.yml not found in container '" + containerId + "' at path: " + path
              + ". Please ensure the Docker image contains exam.yml at the workspace root.");
    }

    public ExamConfigNotFoundException(String containerId, String path, Throwable cause) {
        super("Failed to read exam.yml from container '" + containerId + "' at path: " + path, cause);
    }
}
