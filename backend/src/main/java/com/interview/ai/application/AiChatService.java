package com.interview.ai.application;

import com.interview.ai.AiChatMessageEvent;
import com.interview.ai.domain.ConversationMessage;
import com.interview.ai.infrastructure.persistence.ConversationMessageRepository;
import com.interview.ai.internal.AiModelRegistry;
import com.interview.ai.internal.InterviewWorkspaceTools;
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
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
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
    // AI 工具集（listFiles、readFile、runCommand），透過 tool calling 讓 AI 自動呼叫
    private final InterviewWorkspaceTools workspaceTools;

    public AiChatService(ConversationMessageRepository repository,
                         ChatMemory chatMemory,
                         AiModelRegistry modelRegistry,
                         InterviewModelProvider interviewModelProvider,
                         InterviewAiPolicyProvider aiPolicyProvider,
                         InterviewTimeProvider interviewTimeProvider,
                         ApplicationEventPublisher eventPublisher,
                         InterviewWorkspaceTools workspaceTools) {
        this.repository = repository;
        this.chatMemory = chatMemory;
        this.modelRegistry = modelRegistry;
        this.interviewModelProvider = interviewModelProvider;
        this.aiPolicyProvider = aiPolicyProvider;
        this.interviewTimeProvider = interviewTimeProvider;
        this.eventPublisher = eventPublisher;
        this.workspaceTools = workspaceTools;
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

    /**
     * @param toolSseEmitter 由 AiChatController 提供的 SSE 寫入器（Consumer&lt;String&gt;）。
     *                       工具執行前後透過此 consumer 直接寫入 SSE 串流，讓前端即時顯示 tool 狀態。
     *                       傳入 null 或 no-op consumer 即可停用 tool 事件推送。
     */
    public StreamingChatResult streamChat(UUID interviewId, String userMessage, String modelIdOverride,
                                          Consumer<String> toolSseEmitter) {
        if (!aiPolicyProvider.isAiEnabled(interviewId)) {
            throw new AiDisabledException("此階段 AI 不可用，請獨立完成題目");
        }
        if (interviewTimeProvider.isExpired(interviewId)) {
            throw new InterviewExpiredException("面試時間已到，無法使用 AI");
        }

        String conversationId = interviewId.toString();

        // 將用戶訊息存入記憶
        try {
            chatMemory.add(conversationId, new UserMessage(userMessage));
            eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, MessageType.USER, userMessage));
        } catch (Exception e) {
            log.error("Failed to save user message for interview {}", interviewId, e);
        }

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
        // 設計說明：為何使用 .call() 而非 .stream() 搭配 tool calling？
        // Spring AI 官方文件（2.0）的 Tool Calling 範例皆使用 .call()。
        // streaming 模式下，model 發出 function_call → Spring AI 需中斷 stream → 執行工具 →
        // 送回結果 → 繼續 stream，這個中斷/恢復流程在 milestone 版本中可靠性較低。
        // 解法：用 .call() 確保 tool calling 正確執行，再用 Flux.fromIterable() 將完整回覆
        // 切成小段回傳給前端，保留串流 UX 體驗。
        //
        // tool 事件即時推送：toolSseEmitter 注入 ToolContext，供 InterviewWorkspaceTools
        // 在每個工具執行前後直接寫入 SSE 串流。由於工具在 Flux.defer() 同一執行緒中
        // 順序執行（tool → text chunks），不會與文字 delta 寫入競爭，無需額外同步。
        final var finalPromptSpec = promptSpec;
        // 使用 HashMap 而非 Map.of()，以支援 null 值（toolSseEmitter 可能為 null）
        Map<String, Object> toolContextMap = new HashMap<>();
        toolContextMap.put("interviewId", interviewId.toString());
        if (toolSseEmitter != null) {
            toolContextMap.put("toolSseEmitter", toolSseEmitter);
        }
        Flux<String> stream = Flux.defer(() -> {
                    long t0 = System.currentTimeMillis();
                    log.info("[AI-DIAG] interview={} .call() starting (model={})", interviewId, resolvedModelId);
                    String fullContent = finalPromptSpec
                            .messages(conversationMessages)
                            .tools(workspaceTools)
                            .toolContext(toolContextMap)
                            .call()
                            .content();
                    long elapsed = System.currentTimeMillis() - t0;
                    log.info("[AI-DIAG] interview={} .call() completed in {}ms, content={}",
                            interviewId, elapsed,
                            fullContent == null ? "null" : "length=" + fullContent.length());
                    // .call().content() 可能在多輪 tool calling 後回傳 null（已觀察到此現象），
                    // 給出 fallback 訊息避免前端收到空串流（只有 text-start/text-end 無 text-delta）
                    if (fullContent == null || fullContent.isBlank()) {
                        log.warn("[AI-DIAG] interview={} empty content from model, using fallback", interviewId);
                        fullContent = "（AI 已完成工具分析，但未產生文字回覆。請再試一次或換個方式提問。）";
                    }
                    List<String> chunks = splitIntoChunks(fullContent, 20);
                    log.debug("[AI-DIAG] interview={} split into {} chunks", interviewId, chunks.size());
                    // 切分為小段（每段約 20 字元）模擬 token stream
                    return Flux.fromIterable(chunks);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    try {
                        // 串流完成後儲存 assistant 回覆，properties 帶 model ID 供後續分析使用
                        // AssistantMessage 使用 builder()，因為帶 metadata 的 4-arg constructor 是 protected
                        chatMemory.add(conversationId, AssistantMessage.builder()
                                .content(fullResponse.toString())
                                .properties(Map.of("model", resolvedModelId))
                                .build());
                        log.info("[AI-DIAG] interview={} assistant saved, length={}", interviewId, fullResponse.length());
                        eventPublisher.publishEvent(new AiChatMessageEvent(
                                interviewId, MessageType.ASSISTANT, fullResponse.toString()));
                    } catch (Exception e) {
                        log.error("Failed to save assistant message after streaming", e);
                    }
                })
                .doOnError(e -> log.error("AI stream failed for interview {} (model={})", interviewId, resolvedModelId, e));

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
                    .tools(workspaceTools)
                    .toolContext(Map.of("interviewId", interviewId.toString()))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI chat call failed", e);
            return "AI assistant temporarily unavailable. Please try again later.";
        }
    }

    /**
     * 將字串切分為固定大小的小段，用於模擬 token stream 效果。
     * 前端收到連續的小段並即時渲染，視覺上與真正的 streaming 相同。
     */
    private List<String> splitIntoChunks(String text, int chunkSize) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(i + chunkSize, text.length())));
        }
        return chunks;
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
