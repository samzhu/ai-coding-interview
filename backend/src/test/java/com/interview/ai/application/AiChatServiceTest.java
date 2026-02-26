package com.interview.ai.application;

import com.interview.ai.domain.ConversationMessage;
import com.interview.ai.domain.ConversationRole;
import com.interview.ai.infrastructure.persistence.ConversationMessageRepository;
import com.interview.ai.internal.AiModelRegistry;
import com.interview.interview.InterviewAiPolicyProvider;
import com.interview.interview.InterviewExpiredException;
import com.interview.interview.InterviewModelProvider;
import com.interview.interview.InterviewTimeProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatService 業務邏輯")
class AiChatServiceTest {

    @Mock
    private ConversationMessageRepository repository;

    @Mock
    private AiModelRegistry modelRegistry;

    @Mock
    private InterviewModelProvider interviewModelProvider;

    @Mock
    private InterviewAiPolicyProvider aiPolicyProvider;

    @Mock
    private InterviewTimeProvider interviewTimeProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("chat 應儲存用戶訊息並回傳 AI 回覆")
    void shouldSaveUserMessageAndReturnAiResponse() {
        UUID interviewId = UUID.randomUUID();

        ChatClient chatClient = Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(chatClient.prompt().messages(anyList()).call().content())
                .thenReturn("Think about using a hash map.");
        when(interviewModelProvider.getAiModel(interviewId)).thenReturn("gemini-2.5-flash");
        when(modelRegistry.getChatClient("gemini-2.5-flash")).thenReturn(Optional.of(chatClient));
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);

        AiChatService service = new AiChatService(repository, modelRegistry, interviewModelProvider, aiPolicyProvider, interviewTimeProvider, eventPublisher);

        when(repository.findByInterviewIdOrderByCreatedAtAsc(interviewId)).thenReturn(List.of());
        ConversationMessage saved = ConversationMessage.create(interviewId, ConversationRole.ASSISTANT, "Think about using a hash map.");
        when(repository.save(any())).thenReturn(saved);

        ConversationMessage result = service.chat(interviewId, "How do I solve Two Sum?");

        assertThat(result.getRole()).isEqualTo("ASSISTANT");
        verify(repository, times(2)).save(any(ConversationMessage.class));
    }

    @Test
    @DisplayName("無可用模型時應回傳 stub 回應")
    void shouldReturnStubResponseWhenNoModels() {
        UUID interviewId = UUID.randomUUID();
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);
        when(interviewModelProvider.getAiModel(interviewId)).thenReturn("gemini-2.5-flash");
        when(modelRegistry.getChatClient("gemini-2.5-flash")).thenReturn(Optional.empty());
        when(modelRegistry.getFirstClient()).thenReturn(Optional.empty());

        AiChatService service = new AiChatService(repository, modelRegistry, interviewModelProvider, aiPolicyProvider, interviewTimeProvider, eventPublisher);

        ConversationMessage userMsg = ConversationMessage.create(interviewId, ConversationRole.USER, "Hello");
        ConversationMessage stubMsg = ConversationMessage.create(interviewId, ConversationRole.ASSISTANT,
                "AI assistant is not configured for this environment. Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.");

        when(repository.findByInterviewIdOrderByCreatedAtAsc(interviewId)).thenReturn(List.of(userMsg));
        when(repository.save(any())).thenReturn(stubMsg);

        ConversationMessage result = service.chat(interviewId, "Hello");

        assertThat(result.getRole()).isEqualTo("ASSISTANT");
    }

    @Test
    @DisplayName("getHistory 應回傳對話歷史")
    void shouldReturnConversationHistory() {
        UUID interviewId = UUID.randomUUID();
        AiChatService service = new AiChatService(repository, modelRegistry, interviewModelProvider, aiPolicyProvider, interviewTimeProvider, eventPublisher);

        var msg = ConversationMessage.create(interviewId, ConversationRole.USER, "Hello");
        when(repository.findByInterviewIdOrderByCreatedAtAsc(interviewId)).thenReturn(List.of(msg));

        var history = service.getHistory(interviewId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("streamChat 有 ChatClient 時應回傳串流")
    void shouldStreamResponseWhenChatClientPresent() {
        UUID interviewId = UUID.randomUUID();

        ChatClient chatClient = Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(chatClient.prompt().messages(anyList()).stream().content())
                .thenReturn(Flux.just("Hello", " World"));
        when(interviewModelProvider.getAiModel(interviewId)).thenReturn("gemini-2.5-flash");
        when(modelRegistry.getChatClient("gemini-2.5-flash")).thenReturn(Optional.of(chatClient));
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);

        AiChatService service = new AiChatService(repository, modelRegistry, interviewModelProvider, aiPolicyProvider, interviewTimeProvider, eventPublisher);

        ConversationMessage userMsg = ConversationMessage.create(interviewId, ConversationRole.USER, "Any hints?");
        ConversationMessage savedMsg = ConversationMessage.create(interviewId, ConversationRole.ASSISTANT, "Hello World");
        when(repository.findByInterviewIdOrderByCreatedAtAsc(interviewId)).thenReturn(List.of(userMsg));
        when(repository.save(any())).thenReturn(savedMsg);

        var result = service.streamChat(interviewId, "Any hints?", null);

        assertThat(result.messageId()).isNotNull();

        List<String> tokens = result.tokenStream().collectList().block();
        assertThat(tokens).containsExactly("Hello", " World");

        verify(repository, atLeastOnce()).save(argThat(msg ->
                msg.getRole().equals("ASSISTANT") && msg.getContent().equals("Hello World")));
    }

    @Test
    @DisplayName("AI 停用時 chat() 應拋出 AiDisabledException")
    void shouldThrowWhenAiDisabledForChat() {
        UUID interviewId = UUID.randomUUID();
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(false);
        AiChatService service = new AiChatService(repository, modelRegistry, interviewModelProvider, aiPolicyProvider, interviewTimeProvider, eventPublisher);

        assertThatThrownBy(() -> service.chat(interviewId, "Give me a hint"))
                .isInstanceOf(AiDisabledException.class)
                .hasMessageContaining("AI 不可用");
    }

    @Test
    @DisplayName("AI 停用時 streamChat() 應拋出 AiDisabledException")
    void shouldThrowWhenAiDisabledForStreamChat() {
        UUID interviewId = UUID.randomUUID();
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(false);
        AiChatService service = new AiChatService(repository, modelRegistry, interviewModelProvider, aiPolicyProvider, interviewTimeProvider, eventPublisher);

        assertThatThrownBy(() -> service.streamChat(interviewId, "Give me a hint", null))
                .isInstanceOf(AiDisabledException.class)
                .hasMessageContaining("AI 不可用");
    }

    @Test
    @DisplayName("超時時 chat() 應拋出 InterviewExpiredException")
    void shouldThrowWhenExpiredForChat() {
        UUID interviewId = UUID.randomUUID();
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);
        when(interviewTimeProvider.isExpired(interviewId)).thenReturn(true);
        AiChatService service = new AiChatService(repository, modelRegistry, interviewModelProvider, aiPolicyProvider, interviewTimeProvider, eventPublisher);

        assertThatThrownBy(() -> service.chat(interviewId, "Give me a hint"))
                .isInstanceOf(InterviewExpiredException.class)
                .hasMessageContaining("時間已到");
    }

    @Test
    @DisplayName("超時時 streamChat() 應拋出 InterviewExpiredException")
    void shouldThrowWhenExpiredForStreamChat() {
        UUID interviewId = UUID.randomUUID();
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);
        when(interviewTimeProvider.isExpired(interviewId)).thenReturn(true);
        AiChatService service = new AiChatService(repository, modelRegistry, interviewModelProvider, aiPolicyProvider, interviewTimeProvider, eventPublisher);

        assertThatThrownBy(() -> service.streamChat(interviewId, "Give me a hint", null))
                .isInstanceOf(InterviewExpiredException.class)
                .hasMessageContaining("時間已到");
    }

    @Test
    @DisplayName("streamChat 無可用模型時應回傳 stub 串流")
    void shouldStreamStubWhenNoModels() {
        UUID interviewId = UUID.randomUUID();
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);
        when(interviewModelProvider.getAiModel(interviewId)).thenReturn("gemini-2.5-flash");
        when(modelRegistry.getChatClient("gemini-2.5-flash")).thenReturn(Optional.empty());
        when(modelRegistry.getFirstClient()).thenReturn(Optional.empty());

        AiChatService service = new AiChatService(repository, modelRegistry, interviewModelProvider, aiPolicyProvider, interviewTimeProvider, eventPublisher);

        ConversationMessage userMsg = ConversationMessage.create(interviewId, ConversationRole.USER, "Hello");
        ConversationMessage stubMsg = ConversationMessage.create(interviewId, ConversationRole.ASSISTANT, "AI assistant is not configured");
        when(repository.findByInterviewIdOrderByCreatedAtAsc(interviewId)).thenReturn(List.of(userMsg));
        when(repository.save(any())).thenReturn(stubMsg);

        var result = service.streamChat(interviewId, "Hello", null);

        assertThat(result.messageId()).isNotNull();

        List<String> tokens = result.tokenStream().collectList().block();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0)).contains("AI assistant is not configured");

        verify(repository, times(2)).save(any(ConversationMessage.class));
    }
}
