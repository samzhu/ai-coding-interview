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
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * AI 聊天服務。
 *
 * 架構說明：使用 MessageChatMemoryAdvisor + ToolCallAdvisor 取代手動 chatMemory.add/get 呼叫。
 *
 * 為什麼需要 ToolCallAdvisor？
 * 原本 ChatModel 內部執行 tool calling 循環（讀檔 → 分析 → 繼續對話），
 * 但這個循環完全繞過 advisor chain，中間的 AssistantMessage(toolCalls) 和
 * ToolResponseMessage 永遠不會進入 ChatMemory。下一輪對話時 AI 不知道
 * 自己已讀過哪些檔案，因此不斷重複「我需要讀取檔案」。
 *
 * 加入 ToolCallAdvisor 後，tool calling 循環移到 advisor chain，
 * MessageChatMemoryAdvisor.after() 在所有工具執行完畢後，將完整對話歷史
 * （含 tool 中間訊息）一次存入 DB，解決重複讀檔問題。
 */
@Service
@Transactional
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    // 直接使用 Repository 是為了 getHistory() API 回傳需要 id、createdAt 等 DB 欄位
    private final ConversationMessageRepository repository;
    private final AiModelRegistry modelRegistry;
    private final InterviewModelProvider interviewModelProvider;
    private final InterviewAiPolicyProvider aiPolicyProvider;
    private final InterviewTimeProvider interviewTimeProvider;
    private final ApplicationEventPublisher eventPublisher;
    // AI 工具集（listFiles、readFile、runCommand），透過 tool calling 讓 AI 自動呼叫
    private final InterviewWorkspaceTools workspaceTools;
    // Advisor beans：自動管理 ChatMemory（載入歷史 + 儲存新訊息含 tool 中間訊息）
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final ToolCallAdvisor toolCallAdvisor;

    public AiChatService(ConversationMessageRepository repository,
                         AiModelRegistry modelRegistry,
                         InterviewModelProvider interviewModelProvider,
                         InterviewAiPolicyProvider aiPolicyProvider,
                         InterviewTimeProvider interviewTimeProvider,
                         ApplicationEventPublisher eventPublisher,
                         InterviewWorkspaceTools workspaceTools,
                         MessageChatMemoryAdvisor memoryAdvisor,
                         ToolCallAdvisor toolCallAdvisor) {
        this.repository = repository;
        this.modelRegistry = modelRegistry;
        this.interviewModelProvider = interviewModelProvider;
        this.aiPolicyProvider = aiPolicyProvider;
        this.interviewTimeProvider = interviewTimeProvider;
        this.eventPublisher = eventPublisher;
        this.workspaceTools = workspaceTools;
        this.memoryAdvisor = memoryAdvisor;
        this.toolCallAdvisor = toolCallAdvisor;
    }

    public ConversationMessage chat(UUID interviewId, String userMessage) {
        if (!aiPolicyProvider.isAiEnabled(interviewId)) {
            throw new AiDisabledException("此階段 AI 不可用，請獨立完成題目");
        }
        if (interviewTimeProvider.isExpired(interviewId)) {
            throw new InterviewExpiredException("面試時間已到，無法使用 AI");
        }

        String conversationId = interviewId.toString();
        eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, MessageType.USER, userMessage));

        Optional<ChatClient> chatClientOpt = resolveChatClient(resolveModelId(interviewId, null));
        if (chatClientOpt.isEmpty()) {
            String stub = "AI assistant is not configured for this environment. " +
                    "Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.";
            repository.save(ConversationMessage.create(interviewId, MessageType.USER, userMessage));
            repository.save(ConversationMessage.create(interviewId, MessageType.ASSISTANT, stub));
            List<ConversationMessage> all = repository.findByInterviewIdOrderByCreatedAtAsc(interviewId);
            return all.getLast();
        }

        // Advisor chain 自動：載入歷史（含 SYSTEM prompt）→ 存 user msg →
        // 執行 tool calling 循環（每輪存中間訊息）→ 存 final assistant msg
        String response = chatClientOpt.get().prompt()
                .user(userMessage)
                .advisors(memoryAdvisor, toolCallAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(workspaceTools)
                .toolContext(Map.of("interviewId", interviewId.toString()))
                .call()
                .content();

        if (response != null) {
            eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, MessageType.ASSISTANT, response));
        }

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
        UUID assistantMsgId = UUID.randomUUID();
        String resolvedModelId = resolveModelId(interviewId, modelIdOverride);
        Optional<ChatClient> chatClientOpt = resolveChatClient(resolvedModelId);

        // 發布 USER 訊息事件（DB 寫入由 advisor 在 Flux.defer() 中自動完成）
        try {
            eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, MessageType.USER, userMessage));
        } catch (Exception e) {
            log.error("Failed to publish user message event for interview {}", interviewId, e);
        }

        if (chatClientOpt.isEmpty()) {
            String stub = "AI assistant is not configured for this environment. " +
                    "Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.";
            // 無 advisor 可用，直接寫入 repository
            try {
                repository.save(ConversationMessage.create(interviewId, MessageType.USER, userMessage));
                repository.save(ConversationMessage.create(interviewId, MessageType.ASSISTANT, stub));
            } catch (Exception e) {
                log.error("Failed to save stub messages for interview {}", interviewId, e);
            }
            return new StreamingChatResult(Flux.just(stub), assistantMsgId);
        }

        // 使用 HashMap 而非 Map.of()，以支援 null 值（toolSseEmitter 可能為 null）
        Map<String, Object> toolContextMap = new HashMap<>();
        toolContextMap.put("interviewId", interviewId.toString());
        if (toolSseEmitter != null) {
            toolContextMap.put("toolSseEmitter", toolSseEmitter);
        }

        // 設計說明：Flux.defer() 包裝 .call()（阻塞 API），搭配兩層 timeout 保護：
        // 1. .timeout(AI_CALL_TIMEOUT)：Flux 層超時，避免 boundedElastic thread 被長時間卡住；
        //    超時時發出 TimeoutException，由 doOnError 記錄，Controller 的 error callback 傳友善訊息給前端。
        // 2. Controller 層的 latch.await(LATCH_TIMEOUT)：比 Flux timeout 長 30s，作為最終兜底。
        //
        // tool 事件即時推送：toolSseEmitter 注入 ToolContext，供 InterviewWorkspaceTools
        // 在每個工具執行前後直接寫入 SSE 串流。與 ToolCallAdvisor 的 advisor chain 相容：
        // ToolContext 由 ChatClient 傳遞，不受 advisor 影響。
        final Duration AI_CALL_TIMEOUT = Duration.ofSeconds(90);
        StringBuilder fullResponse = new StringBuilder();

        Flux<String> stream = Flux.defer(() -> {
                    long t0 = System.currentTimeMillis();
                    log.info("[AI-DIAG] interview={} .call() starting (model={})", interviewId, resolvedModelId);

                    // Advisor chain 自動：載入歷史（含 SYSTEM prompt）→ 存 user msg →
                    // 執行 tool calling 循環（每輪存中間訊息）→ 存 final assistant msg
                    // 不需要 .system() — system prompt 已在 ChatMemory 中（AiSessionInitializer 寫入）
                    String fullContent = chatClientOpt.get().prompt()
                            .user(userMessage)
                            .advisors(memoryAdvisor, toolCallAdvisor)
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                            .tools(workspaceTools)
                            .toolContext(toolContextMap)
                            .call()
                            .content();

                    long elapsed = System.currentTimeMillis() - t0;
                    log.info("[AI-DIAG] interview={} .call() completed in {}ms, content={}",
                            interviewId, elapsed,
                            fullContent == null ? "null" : "length=" + fullContent.length());

                    // 設計說明：tool calling 完成後若無文字回覆，顯示友善提示。
                    // 使用 ToolCallAdvisor 後，此情況應較少發生，但保留作為防禦性兜底。
                    if (fullContent == null || fullContent.isBlank()) {
                        log.warn("[AI-DIAG] interview={} empty content after tool calling", interviewId);
                        fullContent = "（AI 已完成工具分析，但未產生文字回覆。請再試一次或換個方式提問。）";
                    }

                    List<String> chunks = splitIntoChunks(fullContent, 20);
                    log.debug("[AI-DIAG] interview={} split into {} chunks", interviewId, chunks.size());
                    return Flux.fromIterable(chunks);
                })
                .timeout(AI_CALL_TIMEOUT)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    try {
                        // Advisor 已將 user/tool/assistant 訊息存入 DB；此處只發布事件供監控使用
                        log.info("[AI-DIAG] interview={} assistant sent, length={}", interviewId, fullResponse.length());
                        eventPublisher.publishEvent(new AiChatMessageEvent(
                                interviewId, MessageType.ASSISTANT, fullResponse.toString()));
                    } catch (Exception e) {
                        log.error("Failed to publish assistant message event after streaming", e);
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
