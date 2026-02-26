package com.interview.ai.application;

import com.interview.ai.AiChatMessageEvent;
import com.interview.ai.domain.ConversationMessage;
import com.interview.ai.infrastructure.persistence.ConversationMessageRepository;
import com.interview.ai.internal.AiModelRegistry;
import com.interview.interview.InterviewAiPolicyProvider;
import com.interview.interview.InterviewExpiredException;
import com.interview.interview.InterviewModelProvider;
import com.interview.interview.InterviewTimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    // ChatMemory 用於 AI 對話上下文管理（歷史寫入 + 讀取），底層使用 ConversationChatMemoryRepository
    private final ChatMemory chatMemory;
    // 直接使用 Repository 是為了 getHistory() API 回傳需要 id、createdAt 等 DB 欄位
    private final ConversationMessageRepository repository;
    private final AiModelRegistry modelRegistry;
    private final InterviewModelProvider interviewModelProvider;
    private final InterviewAiPolicyProvider aiPolicyProvider;
    private final InterviewTimeProvider interviewTimeProvider;
    private final ApplicationEventPublisher eventPublisher;

    public AiChatService(ConversationMessageRepository repository,
                         ChatMemory chatMemory,
                         AiModelRegistry modelRegistry,
                         InterviewModelProvider interviewModelProvider,
                         InterviewAiPolicyProvider aiPolicyProvider,
                         InterviewTimeProvider interviewTimeProvider,
                         ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.chatMemory = chatMemory;
        this.modelRegistry = modelRegistry;
        this.interviewModelProvider = interviewModelProvider;
        this.aiPolicyProvider = aiPolicyProvider;
        this.interviewTimeProvider = interviewTimeProvider;
        this.eventPublisher = eventPublisher;
    }

    public ConversationMessage chat(UUID interviewId, String userMessage) {
        if (!aiPolicyProvider.isAiEnabled(interviewId)) {
            throw new AiDisabledException("此階段 AI 不可用，請獨立完成題目");
        }
        if (interviewTimeProvider.isExpired(interviewId)) {
            throw new InterviewExpiredException("面試時間已到，無法使用 AI");
        }

        String conversationId = interviewId.toString();

        // 將用戶訊息存入記憶（透過 ConversationChatMemoryRepository 寫入 DB）
        chatMemory.add(conversationId, new UserMessage(userMessage));
        eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, MessageType.USER, userMessage));

        // 從記憶取得完整歷史（含 SYSTEM prompt），透過 ChatClient 呼叫 AI
        List<Message> history = chatMemory.get(conversationId);
        String response = generateResponseFromHistory(interviewId, history);

        // 將 AI 回覆存入記憶
        chatMemory.add(conversationId, new AssistantMessage(response));
        eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, MessageType.ASSISTANT, response));

        // 回傳最後儲存的 ConversationMessage（含 id/createdAt），供 REST 回應使用
        List<ConversationMessage> allMessages = repository.findByInterviewIdOrderByCreatedAtAsc(interviewId);
        return allMessages.getLast();
    }

    public record StreamingChatResult(Flux<String> tokenStream, UUID messageId) {}

    public StreamingChatResult streamChat(UUID interviewId, String userMessage, String modelIdOverride) {
        if (!aiPolicyProvider.isAiEnabled(interviewId)) {
            throw new AiDisabledException("此階段 AI 不可用，請獨立完成題目");
        }
        if (interviewTimeProvider.isExpired(interviewId)) {
            throw new InterviewExpiredException("面試時間已到，無法使用 AI");
        }

        String conversationId = interviewId.toString();

        // 將用戶訊息存入記憶
        chatMemory.add(conversationId, new UserMessage(userMessage));
        eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, MessageType.USER, userMessage));

        UUID assistantMsgId = UUID.randomUUID();
        String resolvedModelId = resolveModelId(interviewId, modelIdOverride);
        Optional<ChatClient> chatClient = resolveChatClient(resolvedModelId);

        if (chatClient.isEmpty()) {
            String stub = "AI assistant is not configured for this environment. " +
                    "Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.";
            chatMemory.add(conversationId, new AssistantMessage(stub));
            return new StreamingChatResult(Flux.just(stub), assistantMsgId);
        }

        // 從記憶取得完整歷史，分離 SYSTEM prompt 和對話訊息
        List<Message> history = chatMemory.get(conversationId);
        String systemPrompt = history.stream()
                .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                .map(Message::getText)
                .collect(Collectors.joining("\n"));
        List<Message> conversationMessages = history.stream()
                .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                .toList();

        StringBuilder fullResponse = new StringBuilder();
        var promptSpec = chatClient.get().prompt();
        if (!systemPrompt.isEmpty()) {
            promptSpec = promptSpec.system(systemPrompt);
        }
        Flux<String> stream = promptSpec
                .messages(conversationMessages)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    try {
                        // 串流完成後儲存 assistant 回覆，properties 帶 model ID 供後續分析使用
                        // AssistantMessage 使用 builder()，因為帶 metadata 的 4-arg constructor 是 protected
                        chatMemory.add(conversationId, AssistantMessage.builder()
                                .content(fullResponse.toString())
                                .properties(Map.of("model", resolvedModelId))
                                .build());
                        eventPublisher.publishEvent(new AiChatMessageEvent(
                                interviewId, MessageType.ASSISTANT, fullResponse.toString()));
                    } catch (Exception e) {
                        log.error("Failed to save assistant message after streaming", e);
                    }
                })
                .doOnError(e -> log.error("AI stream failed for interview {}", interviewId, e));

        return new StreamingChatResult(stream, assistantMsgId);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> getHistory(UUID interviewId) {
        // 直接查詢 repository 而非透過 ChatMemory，因為 API 回傳需要 id、createdAt 等 DB 欄位
        return repository.findByInterviewIdOrderByCreatedAtAsc(interviewId);
    }

    private String generateResponseFromHistory(UUID interviewId, List<Message> history) {
        Optional<ChatClient> chatClient = resolveChatClient(resolveModelId(interviewId, null));
        if (chatClient.isEmpty()) {
            log.warn("No ChatClient available for interview {}. Returning stub response.", interviewId);
            return "AI assistant is not configured for this environment. " +
                    "Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.";
        }

        try {
            // 分離 SYSTEM prompt 和對話訊息（ChatClient API 需要分開傳入）
            String systemPrompt = history.stream()
                    .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                    .map(Message::getText)
                    .collect(Collectors.joining("\n"));
            List<Message> conversationMessages = history.stream()
                    .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                    .toList();

            var promptSpec = chatClient.get().prompt();
            if (!systemPrompt.isEmpty()) {
                promptSpec = promptSpec.system(systemPrompt);
            }
            return promptSpec
                    .messages(conversationMessages)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI chat call failed", e);
            return "AI assistant temporarily unavailable. Please try again later.";
        }
    }

    private String resolveModelId(UUID interviewId, String modelIdOverride) {
        // 有 override 時優先使用，否則從 interview 設定取得
        if (modelIdOverride != null && !modelIdOverride.isBlank()) {
            return modelIdOverride;
        }
        return interviewModelProvider.getAiModel(interviewId);
    }

    private Optional<ChatClient> resolveChatClient(String modelId) {
        Optional<ChatClient> client = modelRegistry.getChatClient(modelId);
        if (client.isEmpty()) {
            log.warn("Model '{}' not found in registry, trying first available", modelId);
            return modelRegistry.getFirstClient();
        }
        return client;
    }
}
