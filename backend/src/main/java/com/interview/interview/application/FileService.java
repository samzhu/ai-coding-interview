package com.interview.interview.application;

import com.interview.execution.CodeExecutionResult;
import com.interview.execution.ContainerFile;
import com.interview.execution.ContainerService;
import com.interview.execution.ExamConfig;
import com.interview.execution.ExamConfigService;
import com.interview.interview.InterviewFileProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FileService implements InterviewFileProvider {

    private final InterviewService interviewService;
    private final ContainerService containerService;
    private final ExamConfigService examConfigService;

    public FileService(InterviewService interviewService,
                       ContainerService containerService,
                       ExamConfigService examConfigService) {
        this.interviewService = interviewService;
        this.containerService = containerService;
        this.examConfigService = examConfigService;
    }

    public List<ContainerFile> listFiles(UUID interviewId) {
        String containerId = interviewService.ensureContainerRunning(interviewId);
        ExamConfig examConfig = examConfigService.getExamConfig(containerId);
        String workspace = examConfig.effectiveWorkspace();
        List<ContainerFile> all = containerService.listFiles(containerId, workspace);
        List<String> excludePatterns = examConfig.exclude();
        return all.stream()
                .filter(f -> !isExcluded(f.filePath(), workspace, excludePatterns))
                .toList();
    }

    private boolean isExcluded(String filePath, String workspace, List<String> patterns) {
        String relative = filePath.startsWith(workspace + "/")
                ? filePath.substring(workspace.length() + 1) : filePath;
        for (String pattern : patterns) {
            if (pattern.endsWith("/")) {
                String dir = pattern.substring(0, pattern.length() - 1);
                if (relative.startsWith(dir + "/") || relative.equals(dir)) return true;
            } else if (pattern.startsWith("*.")) {
                if (relative.endsWith(pattern.substring(1))) return true;
            } else {
                String fileName = relative.contains("/")
                        ? relative.substring(relative.lastIndexOf("/") + 1) : relative;
                if (fileName.equals(pattern)) return true;
            }
        }
        return false;
    }

    public String readFile(UUID interviewId, String path) {
        String containerId = interviewService.ensureContainerRunning(interviewId);
        ExamConfig examConfig = examConfigService.getExamConfig(containerId);
        String workspace = examConfig.effectiveWorkspace();
        String fullPath = path.startsWith("/") ? path : workspace + "/" + path;
        return containerService.readFile(containerId, fullPath);
    }

    public void writeFile(UUID interviewId, String path, String content) {
        String containerId = interviewService.ensureContainerRunning(interviewId);
        ExamConfig examConfig = examConfigService.getExamConfig(containerId);
        String workspace = examConfig.effectiveWorkspace();
        String fullPath = path.startsWith("/") ? path : workspace + "/" + path;
        containerService.writeFile(containerId, fullPath, content);
    }

    /**
     * 在工作區目錄下執行 shell 指令。
     * 自動 cd 至 workspace 確保相對路徑正確，並套用指定的 timeout。
     */
    @Override
    public CodeExecutionResult execInWorkspace(UUID interviewId, String command, int timeoutSeconds) {
        String containerId = interviewService.ensureContainerRunning(interviewId);
        ExamConfig examConfig = examConfigService.getExamConfig(containerId);
        String workspace = examConfig.effectiveWorkspace();
        String fullCommand = "cd " + workspace + " && " + command;
        return containerService.execCommand(containerId, fullCommand, timeoutSeconds);
    }
}
