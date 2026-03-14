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
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private InterviewWorkspaceTools workspaceTools;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private ToolCallingManager toolCallingManager;

    private AiChatService buildService() {
        return new AiChatService(repository, modelRegistry,
                interviewModelProvider, aiPolicyProvider, interviewTimeProvider, eventPublisher,
                workspaceTools, chatMemory, toolCallingManager);
    }

    /** 建立一個 no-tool-calls ChatResponse mock，回傳指定文字。
     *  設計說明：先 stub getOutput() 回傳真實 AssistantMessage，不再 stub .getText()，
     *  因為 AssistantMessage(text).getText() 本身就會回傳 text。
     *  metadata 透過 RETURNS_DEEP_STUBS 處理，updateLastAssistantTokenUsage 有 try-catch 兜底。
     */
    private ChatResponse mockChatResponse(String text) {
        ChatResponse response = Mockito.mock(ChatResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(response.hasToolCalls()).thenReturn(false);
        when(response.getResult().getOutput()).thenReturn(new AssistantMessage(text));
        return response;
    }

    @Test
    @DisplayName("chat 有 ChatModel 時應回傳最後一筆訊息")
    void shouldSaveUserMessageAndReturnAiResponse() {
        UUID interviewId = UUID.randomUUID();

        ChatModel chatModel = Mockito.mock(ChatModel.class);
        ChatResponse chatResponse = mockChatResponse("Think about using a hash map.");
        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
        when(interviewModelProvider.getAiModel(interviewId)).thenReturn("gemini-2.5-flash");
        when(modelRegistry.getChatModel("gemini-2.5-flash")).thenReturn(Optional.of(chatModel));
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);
        when(chatMemory.get(interviewId.toString())).thenReturn(List.of());

        ConversationMessage assistantMsg = ConversationMessage.create(
                interviewId, MessageType.ASSISTANT, "Think about using a hash map.");
        when(repository.findByInterviewIdOrderByCreatedAtAsc(interviewId)).thenReturn(List.of(assistantMsg));

        AiChatService service = buildService();
        ConversationMessage result = service.chat(interviewId, "How do I solve Two Sum?");

        assertThat(result.getRole()).isEqualTo("ASSISTANT");
        // 驗證最後從 repository 查詢（取得含 id/createdAt 的 DB 物件）
        verify(repository).findByInterviewIdOrderByCreatedAtAsc(interviewId);
    }

    @Test
    @DisplayName("無可用模型時 chat() 應回傳 stub 回應並直接寫入 repository")
    void shouldReturnStubResponseWhenNoModels() {
        UUID interviewId = UUID.randomUUID();
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);
        when(interviewModelProvider.getAiModel(interviewId)).thenReturn("gemini-2.5-flash");
        when(modelRegistry.getChatModel("gemini-2.5-flash")).thenReturn(Optional.empty());
        when(modelRegistry.getFirstChatModel()).thenReturn(Optional.empty());

        ConversationMessage assistantMsg = ConversationMessage.create(interviewId, MessageType.ASSISTANT,
                "AI assistant is not configured for this environment. Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.");
        when(repository.findByInterviewIdOrderByCreatedAtAsc(interviewId)).thenReturn(List.of(assistantMsg));

        AiChatService service = buildService();
        ConversationMessage result = service.chat(interviewId, "Hello");

        assertThat(result.getRole()).isEqualTo("ASSISTANT");
        // 無可用模型時直接寫入 repository（user + stub assistant）
        verify(repository, times(2)).save(any(ConversationMessage.class));
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
    @DisplayName("streamChat 有 ChatModel 時應回傳串流")
    void shouldStreamResponseWhenChatClientPresent() {
        UUID interviewId = UUID.randomUUID();

        ChatModel chatModel = Mockito.mock(ChatModel.class);
        ChatResponse chatResponse = mockChatResponse("Hello World");
        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
        when(interviewModelProvider.getAiModel(interviewId)).thenReturn("gemini-2.5-flash");
        when(modelRegistry.getChatModel("gemini-2.5-flash")).thenReturn(Optional.of(chatModel));
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);
        when(chatMemory.get(interviewId.toString())).thenReturn(List.of());

        AiChatService service = buildService();
        var result = service.streamChat(interviewId, "Any hints?", null, null);

        assertThat(result.messageId()).isNotNull();

        // splitIntoChunks("Hello World", 20)：11 字元 < 20，整個字串為一個 chunk
        List<String> tokens = result.tokenStream().collectList().block();
        assertThat(tokens).containsExactly("Hello World");
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

        assertThatThrownBy(() -> service.streamChat(interviewId, "Give me a hint", null, null))
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

        assertThatThrownBy(() -> service.streamChat(interviewId, "Give me a hint", null, null))
                .isInstanceOf(InterviewExpiredException.class)
                .hasMessageContaining("時間已到");
    }

    @Test
    @DisplayName("streamChat 無可用模型時應回傳 stub 串流並直接寫入 repository")
    void shouldStreamStubWhenNoModels() {
        UUID interviewId = UUID.randomUUID();
        when(aiPolicyProvider.isAiEnabled(interviewId)).thenReturn(true);
        when(interviewModelProvider.getAiModel(interviewId)).thenReturn("gemini-2.5-flash");
        when(modelRegistry.getChatModel("gemini-2.5-flash")).thenReturn(Optional.empty());
        when(modelRegistry.getFirstChatModel()).thenReturn(Optional.empty());

        AiChatService service = buildService();
        var result = service.streamChat(interviewId, "Hello", null, null);

        assertThat(result.messageId()).isNotNull();

        List<String> tokens = result.tokenStream().collectList().block();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0)).contains("AI assistant is not configured");

        // 無 advisor 時直接寫入 repository（user + stub assistant）
        verify(repository, times(2)).save(any(ConversationMessage.class));
    }
}
