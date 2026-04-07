package com.interview.scoring.application;

import com.interview.ai.AiChatClientLookup;
import com.interview.scoring.domain.PilotJudgement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * 呼叫 Gemini 2.5 Pro 對 timeline 做 holistic 評分。
 *
 * 設計說明：
 * - 用 Spring AI 的 .entity(PilotJudgement.class) 做 structured output binding。
 *   這個 API 在 Spring AI 2.0.0-M4 支援 google-genai provider。
 * - System prompt 從 classpath resource 載入，方便版本管理（每次改 prompt 同時 bump version）。
 * - 透過 AiChatClientLookup 取得 ChatClient，避免跨模組直接 inject AiModelRegistry。
 * - 若 model 未配置或呼叫失敗，拋例外讓上層 (ScoringOrchestrator) 用 InterviewScore.failed() 處理。
 */
@Component
public class GeminiPilotJudge {

    private static final Logger log = LoggerFactory.getLogger(GeminiPilotJudge.class);
    private static final String JUDGE_MODEL_ID = "gemini-2.5-pro";

    private final AiChatClientLookup chatClientLookup;
    private final String systemPrompt;

    @Autowired
    GeminiPilotJudge(AiChatClientLookup chatClientLookup) {
        this.chatClientLookup = chatClientLookup;
        this.systemPrompt = loadSystemPrompt();
    }

    // Test-only constructor — package-private
    GeminiPilotJudge(AiChatClientLookup chatClientLookup, String systemPrompt) {
        this.chatClientLookup = chatClientLookup;
        this.systemPrompt = systemPrompt;
    }

    public PilotJudgement judge(String timelineText) {
        var client = chatClientLookup.findByModelId(JUDGE_MODEL_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Judge model '" + JUDGE_MODEL_ID + "' not configured — check AciModelsProperties + GOOGLE_GENAI_API_KEY"));

        log.info("Calling Gemini judge with timeline ({} chars)", timelineText.length());

        return client.prompt()
                .system(systemPrompt)
                .user(timelineText)
                .call()
                .entity(PilotJudgement.class);
    }

    private static String loadSystemPrompt() {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource("prompts/pilot-judge-system-prompt.txt").getInputStream(),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load pilot judge system prompt resource", e);
        }
    }
}
