package com.interview.interview.application;

import com.interview.execution.ContainerFile;
import com.interview.execution.ContainerService;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import com.interview.question.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FileService {

    private final InterviewRepository interviewRepository;
    private final QuestionService questionService;
    private final ContainerService containerService;

    public FileService(InterviewRepository interviewRepository,
                       QuestionService questionService,
                       ContainerService containerService) {
        this.interviewRepository = interviewRepository;
        this.questionService = questionService;
        this.containerService = containerService;
    }

    public List<ContainerFile> listFiles(UUID interviewId) {
        var interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        var question = questionService.getQuestion(interview.getQuestionId());
        String containerId = interview.getContainerId();
        if (containerId == null || containerId.isBlank()) {
            throw new IllegalStateException("No container running for interview: " + interviewId);
        }
        String workspace = question.workspace() != null ? question.workspace() : "/workspace";
        return containerService.listFiles(containerId, workspace);
    }

    public String readFile(UUID interviewId, String path) {
        var interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        var question = questionService.getQuestion(interview.getQuestionId());
        String containerId = interview.getContainerId();
        if (containerId == null || containerId.isBlank()) {
            throw new IllegalStateException("No container running for interview: " + interviewId);
        }
        String workspace = question.workspace() != null ? question.workspace() : "/workspace";
        String fullPath = path.startsWith("/") ? path : workspace + "/" + path;
        return containerService.readFile(containerId, fullPath);
    }

    @Transactional
    public void writeFile(UUID interviewId, String path, String content) {
        var interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        var question = questionService.getQuestion(interview.getQuestionId());
        String containerId = interview.getContainerId();
        if (containerId == null || containerId.isBlank()) {
            throw new IllegalStateException("No container running for interview: " + interviewId);
        }
        String workspace = question.workspace() != null ? question.workspace() : "/workspace";
        String fullPath = path.startsWith("/") ? path : workspace + "/" + path;
        containerService.writeFile(containerId, fullPath, content);
    }
}
