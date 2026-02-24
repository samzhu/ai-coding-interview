package com.interview.interview;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 跨模組公開 API：查詢當前 Checkpoint 的 AI 政策。
 * 所有考試都預設可用 AI，永遠回傳 true。
 */
@Service
public class InterviewAiPolicyProvider {

    public boolean isAiEnabled(UUID interviewId) {
        return true;
    }
}
