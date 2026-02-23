package com.interview.interview;

import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import com.interview.question.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 跨模組公開 API：查詢當前 Checkpoint 的 AI 政策。
 * ai 模組透過此介面判斷是否允許 AI 對話。
 */
@Service
public class InterviewAiPolicyProvider {

    private final InterviewRepository interviewRepository;
    private final CheckpointResultRepository checkpointResultRepository;
    private final QuestionService questionService;

    public InterviewAiPolicyProvider(InterviewRepository interviewRepository,
                                     CheckpointResultRepository checkpointResultRepository,
                                     QuestionService questionService) {
        this.interviewRepository = interviewRepository;
        this.checkpointResultRepository = checkpointResultRepository;
        this.questionService = questionService;
    }

    /**
     * 判斷指定面試的當前 Checkpoint 是否允許 AI 對話。
     *
     * @param interviewId 面試 ID
     * @return true 表示 AI 可用；false 表示此階段停用 AI
     */
    @Transactional(readOnly = true)
    public boolean isAiEnabled(UUID interviewId) {
        var interview = interviewRepository.findById(interviewId).orElse(null);
        if (interview == null) return true;

        var currentResult = checkpointResultRepository.findCurrentCheckpoint(interviewId).orElse(null);
        if (currentResult == null) return true;

        var question = questionService.getQuestion(interview.getQuestionId());
        return question.checkpoints().stream()
                .filter(cp -> cp.id().equals(currentResult.getCheckpointId()))
                .findFirst()
                .map(cp -> cp.aiEnabled())
                .orElse(true);
    }
}
