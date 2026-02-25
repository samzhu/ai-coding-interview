package com.interview.interview.application;

import com.interview.execution.ContainerFile;
import com.interview.execution.ContainerService;
import com.interview.question.QuestionDetail;
import com.interview.question.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FileService {

    private final InterviewService interviewService;
    private final QuestionService questionService;
    private final ContainerService containerService;

    public FileService(InterviewService interviewService,
                       QuestionService questionService,
                       ContainerService containerService) {
        this.interviewService = interviewService;
        this.questionService = questionService;
        this.containerService = containerService;
    }

    public List<ContainerFile> listFiles(UUID interviewId) {
        String containerId = interviewService.ensureContainerRunning(interviewId);
        var interview = interviewService.findById(interviewId);
        QuestionDetail question = questionService.getQuestion(interview.getQuestionId());
        String workspace = question.workspace() != null ? question.workspace() : "/workspace";
        List<ContainerFile> all = containerService.listFiles(containerId, workspace);
        List<String> excludePatterns = question.exclude();
        if (excludePatterns == null || excludePatterns.isEmpty()) {
            return all;
        }
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
        var interview = interviewService.findById(interviewId);
        var question = questionService.getQuestion(interview.getQuestionId());
        String workspace = question.workspace() != null ? question.workspace() : "/workspace";
        String fullPath = path.startsWith("/") ? path : workspace + "/" + path;
        return containerService.readFile(containerId, fullPath);
    }

    public void writeFile(UUID interviewId, String path, String content) {
        String containerId = interviewService.ensureContainerRunning(interviewId);
        var interview = interviewService.findById(interviewId);
        var question = questionService.getQuestion(interview.getQuestionId());
        String workspace = question.workspace() != null ? question.workspace() : "/workspace";
        String fullPath = path.startsWith("/") ? path : workspace + "/" + path;
        containerService.writeFile(containerId, fullPath, content);
    }
}
