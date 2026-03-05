package com.interview.ai.application;

import com.interview.ai.domain.ConversationMessage;
import com.interview.ai.infrastructure.persistence.ConversationMessageRepository;
import com.interview.ai.internal.AiModelRegistry;
import com.interview.ai.internal.InterviewWorkspaceTools;
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
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatService 業務邏輯")
class AiChatServiceTest {

    @Mock
    private ConversationMessageRepository repository;

    @Mock
    private ChatMemory chatMemory;

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

    @Mock
    private InterviewWorkspaceTools workspaceTools;

    private AiChatService buildService() {
        return new AiChatService(repository, chatMemory, modelRegistry,
                interviewModelProvider, aiPolicyProvider, interviewTimeProvider, eventPublisher,
                workspaceTools);
    }

    @Test
    @DisplayName("chat 應存入用戶訊息並回傳 AI 回覆")
    void shouldSaveUserMessageAndReturnAiResponse() {
        UUID interviewId = UUID.randomUUID();

        ChatClient chatClient = Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        // 鏈中加入 .tools().toolContext() — 與 AiChatService 實際呼叫一致
        when(chatClient.prompt().messages(anyList()).tools(workspaceTools).toolContext(any()).call().content())
                .thenReturn("Think about using a hash map.");
        when(interviewModelProvider.getAiModel(interviewId)).thenReturn("gemini-2.5-flash");
        when(modelRegistry.getChatClient("gemini-2.5-flash")).thenReturn(Optional.of(chatClient));
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);

        // chatMemory.get() 回傳空歷史（無 SYSTEM prompt），對話訊息為空
        when(chatMemory.get(interviewId.toString())).thenReturn(List.of());

        // 最後查詢 repository 以取得含 id/createdAt 的回傳物件
        ConversationMessage assistantMsg = ConversationMessage.create(
                interviewId, MessageType.ASSISTANT, "Think about using a hash map.");
        when(repository.findByInterviewIdOrderByCreatedAtAsc(interviewId)).thenReturn(List.of(assistantMsg));

        AiChatService service = buildService();
        ConversationMessage result = service.chat(interviewId, "How do I solve Two Sum?");

        assertThat(result.getRole()).isEqualTo("ASSISTANT");
        // 驗證 chatMemory.add() 被呼叫了 2 次（user + assistant）
        verify(chatMemory, times(2)).add(eq(interviewId.toString()), any(Message.class));
    }

    @Test
    @DisplayName("無可用模型時應回傳 stub 回應")
    void shouldReturnStubResponseWhenNoModels() {
        UUID interviewId = UUID.randomUUID();
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);
        when(interviewModelProvider.getAiModel(interviewId)).thenReturn("gemini-2.5-flash");
        when(modelRegistry.getChatClient("gemini-2.5-flash")).thenReturn(Optional.empty());
        when(modelRegistry.getFirstClient()).thenReturn(Optional.empty());

        when(chatMemory.get(interviewId.toString())).thenReturn(List.of());

        ConversationMessage assistantMsg = ConversationMessage.create(interviewId, MessageType.ASSISTANT,
                "AI assistant is not configured for this environment. Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.");
        when(repository.findByInterviewIdOrderByCreatedAtAsc(interviewId)).thenReturn(List.of(assistantMsg));

        AiChatService service = buildService();
        ConversationMessage result = service.chat(interviewId, "Hello");

        assertThat(result.getRole()).isEqualTo("ASSISTANT");
    }

    @Test
    @DisplayName("getHistory 應回傳對話歷史")
    void shouldReturnConversationHistory() {
        UUID interviewId = UUID.randomUUID();
        AiChatService service = buildService();

        var msg = ConversationMessage.create(interviewId, MessageType.USER, "Hello");
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
        // 底層改為 .call() 確保 tool calling 正確執行（streaming + tool calling 可靠性問題）
        // 對應 AiChatService 的 Flux.defer(() -> ... .call().content()) 路徑
        when(chatClient.prompt().messages(anyList()).tools(workspaceTools).toolContext(any()).call().content())
                .thenReturn("Hello World");
        when(interviewModelProvider.getAiModel(interviewId)).thenReturn("gemini-2.5-flash");
        when(modelRegistry.getChatClient("gemini-2.5-flash")).thenReturn(Optional.of(chatClient));
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);

        // chatMemory.get() 回傳空歷史（無 SYSTEM prompt）
        when(chatMemory.get(interviewId.toString())).thenReturn(List.of());

        AiChatService service = buildService();
        var result = service.streamChat(interviewId, "Any hints?", null);

        assertThat(result.messageId()).isNotNull();

        // splitIntoChunks("Hello World", 20)：11 字元 < 20，整個字串為一個 chunk
        List<String> tokens = result.tokenStream().collectList().block();
        assertThat(tokens).containsExactly("Hello World");

        // 驗證 doOnComplete 後 chatMemory.add() 被呼叫（存入 assistant 回覆）
        // 明確指定 Message 型別避免 ChatMemory.add(String, Message) 與 add(String, List) 歧義
        verify(chatMemory, atLeastOnce()).add(eq(interviewId.toString()),
                (Message) argThat(msg -> msg instanceof AssistantMessage am
                        && am.getText().equals("Hello World")));
    }

    @Test
    @DisplayName("AI 停用時 chat() 應拋出 AiDisabledException")
    void shouldThrowWhenAiDisabledForChat() {
        UUID interviewId = UUID.randomUUID();
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(false);
        AiChatService service = buildService();

        assertThatThrownBy(() -> service.chat(interviewId, "Give me a hint"))
                .isInstanceOf(AiDisabledException.class)
                .hasMessageContaining("AI 不可用");
    }

    @Test
    @DisplayName("AI 停用時 streamChat() 應拋出 AiDisabledException")
    void shouldThrowWhenAiDisabledForStreamChat() {
        UUID interviewId = UUID.randomUUID();
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(false);
        AiChatService service = buildService();

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
        AiChatService service = buildService();

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
        AiChatService service = buildService();

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

        AiChatService service = buildService();
        var result = service.streamChat(interviewId, "Hello", null);

        assertThat(result.messageId()).isNotNull();

        List<String> tokens = result.tokenStream().collectList().block();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0)).contains("AI assistant is not configured");

        // 驗證 chatMemory.add() 被呼叫：user message + stub assistant message
        verify(chatMemory, times(2)).add(eq(interviewId.toString()), any(Message.class));
    }
}
