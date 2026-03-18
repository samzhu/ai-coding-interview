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

    // Resolves the three repeated steps (container → examConfig → workspace/exclude) into one call.
    private record WorkspaceContext(String containerId, String workspace, List<String> excludePatterns) {}

    private WorkspaceContext resolveWorkspace(UUID interviewId) {
        String containerId = interviewService.ensureContainerRunning(interviewId);
        ExamConfig examConfig = examConfigService.getExamConfig(containerId);
        return new WorkspaceContext(containerId, examConfig.effectiveWorkspace(), examConfig.exclude());
    }

    private String resolveFullPath(String workspace, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return workspace;
        return relativePath.startsWith("/") ? relativePath : workspace + "/" + relativePath;
    }

    private List<ContainerFile> filterExcluded(List<ContainerFile> files, String workspace, List<String> patterns) {
        return files.stream()
                .filter(f -> !isExcluded(f.filePath(), workspace, patterns))
                .toList();
    }

    public List<ContainerFile> listDirectory(UUID interviewId, String relativePath) {
        WorkspaceContext ctx = resolveWorkspace(interviewId);
        String fullPath = resolveFullPath(ctx.workspace(), relativePath);
        return filterExcluded(
                containerService.listDirectory(ctx.containerId(), fullPath),
                ctx.workspace(), ctx.excludePatterns())
            .stream()
            .map(f -> new ContainerFile(
                    stripWorkspacePrefix(f.filePath(), ctx.workspace()),
                    f.isDirectory(), f.size()))
            .toList();
    }

    public List<ContainerFile> directoryTree(UUID interviewId, int maxDepth) {
        WorkspaceContext ctx = resolveWorkspace(interviewId);
        return filterExcluded(
                containerService.directoryTree(ctx.containerId(), ctx.workspace(), maxDepth),
                ctx.workspace(), ctx.excludePatterns())
            .stream()
            .map(f -> new ContainerFile(
                    stripWorkspacePrefix(f.filePath(), ctx.workspace()),
                    f.isDirectory(), f.size()))
            .toList();
    }

    // Converts "/workspace/src/main" → "src/main" so AI tools receive workspace-relative paths.
    private String stripWorkspacePrefix(String filePath, String workspace) {
        if (filePath.startsWith(workspace + "/")) {
            return filePath.substring(workspace.length() + 1);
        }
        if (filePath.startsWith(workspace)) {
            return filePath.substring(workspace.length());
        }
        return filePath;
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
