package com.interview.scoring.application;

import com.interview.ai.AiChatClientLookup;
import com.interview.scoring.domain.PilotJudgement;
import com.interview.scoring.domain.PilotVerdict;
import com.interview.scoring.domain.SubScore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiPilotJudgeTest {

    @Mock AiChatClientLookup chatClientLookup;

    // 設計說明：ChatClient 是 fluent API，必須用 RETURNS_DEEP_STUBS 才能 mock 整條 call chain
    @Mock(answer = RETURNS_DEEP_STUBS)
    ChatClient chatClient;

    @Test
    void judge_returnsParsedPilotJudgement_whenChatClientResponds() {
        when(chatClientLookup.findByModelId("gemini-3.1-pro-preview")).thenReturn(Optional.of(chatClient));
        var sample = sampleJudgement();
        when(chatClient.prompt().system(any(String.class)).user(any(String.class)).call()
                .entity(PilotJudgement.class)).thenReturn(sample);

        var judge = new GeminiPilotJudge(chatClientLookup, "fake-system-prompt");
        var result = judge.judge("=== INTERVIEW TIMELINE ===\n[00:00] INTERVIEW_START\n");

        assertThat(result).isEqualTo(sample);
    }

    @Test
    void judge_throws_whenJudgeModelNotConfigured() {
        when(chatClientLookup.findByModelId("gemini-3.1-pro-preview")).thenReturn(Optional.empty());

        var judge = new GeminiPilotJudge(chatClientLookup, "fake-system-prompt");

        assertThatThrownBy(() -> judge.judge("any timeline"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gemini-3.1-pro-preview");
    }

    private static PilotJudgement sampleJudgement() {
        var sub = new SubScore(3.5, "good", List.of(), List.of());
        return new PilotJudgement(
                3.5, PilotVerdict.DRIVER, "headline", "summary",
                new PilotJudgement.SubDimensions(sub, sub, sub, sub));
    }
}
