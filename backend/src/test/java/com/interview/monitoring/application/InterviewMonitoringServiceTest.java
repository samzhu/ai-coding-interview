package com.interview.monitoring.application;

import com.interview.ai.AiChatMessageEvent;
import com.interview.interview.CodeSubmittedEvent;
import com.interview.interview.InterviewCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewMonitoringService 業務邏輯")
class InterviewMonitoringServiceTest {

    private final InterviewMonitoringService service = new InterviewMonitoringService();

    @Test
    @DisplayName("subscribe 應回傳 SseEmitter 並加入 emitter 清單")
    void shouldSubscribeAndReturnEmitter() {
        UUID interviewId = UUID.randomUUID();

        SseEmitter emitter = service.subscribe(interviewId);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("多個 subscribe 同一面試應各自取得 emitter")
    void shouldAllowMultipleSubscribersForSameInterview() {
        UUID interviewId = UUID.randomUUID();

        SseEmitter emitter1 = service.subscribe(interviewId);
        SseEmitter emitter2 = service.subscribe(interviewId);

        assertThat(emitter1).isNotNull();
        assertThat(emitter2).isNotNull();
        assertThat(emitter1).isNotSameAs(emitter2);
    }

    @Test
    @DisplayName("onCodeSubmitted 通過時不拋出例外（無訂閱者情境）")
    void onCodeSubmittedPassedShouldNotThrow() {
        UUID interviewId = UUID.randomUUID();
        String checkpointId = "1";
        CodeSubmittedEvent event = new CodeSubmittedEvent(interviewId, checkpointId, 1, true);

        // 無訂閱者時 broadcast 應靜默成功
        service.onCodeSubmitted(event);
    }

    @Test
    @DisplayName("onCodeSubmitted 失敗時不拋出例外（無訂閱者情境）")
    void onCodeSubmittedFailedShouldNotThrow() {
        UUID interviewId = UUID.randomUUID();
        String checkpointId = "1";
        CodeSubmittedEvent event = new CodeSubmittedEvent(interviewId, checkpointId, 1, false);

        service.onCodeSubmitted(event);
    }

    @Test
    @DisplayName("onAiChatMessage USER 角色不拋出例外")
    void onAiChatMessageUserShouldNotThrow() {
        UUID interviewId = UUID.randomUUID();
        AiChatMessageEvent event = new AiChatMessageEvent(interviewId, "USER", "Hello?");

        service.onAiChatMessage(event);
    }

    @Test
    @DisplayName("onAiChatMessage ASSISTANT 角色不拋出例外")
    void onAiChatMessageAssistantShouldNotThrow() {
        UUID interviewId = UUID.randomUUID();
        AiChatMessageEvent event = new AiChatMessageEvent(interviewId, "ASSISTANT", "Think about hash maps.");

        service.onAiChatMessage(event);
    }

    @Test
    @DisplayName("onInterviewCompleted 不拋出例外（無訂閱者情境）")
    void onInterviewCompletedShouldNotThrow() {
        UUID interviewId = UUID.randomUUID();
        InterviewCompletedEvent event = new InterviewCompletedEvent(interviewId);

        service.onInterviewCompleted(event);
    }

    @Test
    @DisplayName("notifyCodeChange 無訂閱者時應靜默成功")
    void notifyCodeChangeShouldNotThrowWhenNoSubscribers() {
        UUID interviewId = UUID.randomUUID();

        service.notifyCodeChange(interviewId, "main.py", "print('hello')");
    }
}
