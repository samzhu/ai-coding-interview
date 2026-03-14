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
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
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

/**
 * AI 聊天服務。
 *
 * 架構說明：採用 User Controlled Tool Execution（手動 tool loop + 手動 ChatMemory 管理）。
 *
 * Spring AI 官方文件明確指出：MessageChatMemoryAdvisor + ToolCallAdvisor 無法可靠持久化
 * tool 中間訊息（"Currently, the intermediate messages exchanged with a large-language model
 * when performing tool calls are not stored in the memory."）。
 * 參考：https://docs.spring.io/spring-ai/reference/api/tools.html#_user_controlled_tool_execution
 *
 * 改為直接呼叫 ChatModel.call(Prompt) + ToolCallingManager.executeToolCalls()，
 * 在每個步驟後手動 chatMemory.add()，確保 user、ASSISTANT(tool_calls)、
 * ToolResponseMessage 及最終 ASSISTANT 回應全部持久化到 DB。
 */
@Service
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
    private final ChatMemory chatMemory;
    private final ToolCallingManager toolCallingManager;

    public AiChatService(ConversationMessageRepository repository,
                         AiModelRegistry modelRegistry,
                         InterviewModelProvider interviewModelProvider,
                         InterviewAiPolicyProvider aiPolicyProvider,
                         InterviewTimeProvider interviewTimeProvider,
                         ApplicationEventPublisher eventPublisher,
                         InterviewWorkspaceTools workspaceTools,
                         ChatMemory chatMemory,
                         ToolCallingManager toolCallingManager) {
        this.repository = repository;
        this.modelRegistry = modelRegistry;
        this.interviewModelProvider = interviewModelProvider;
        this.aiPolicyProvider = aiPolicyProvider;
        this.interviewTimeProvider = interviewTimeProvider;
        this.eventPublisher = eventPublisher;
        this.workspaceTools = workspaceTools;
        this.chatMemory = chatMemory;
        this.toolCallingManager = toolCallingManager;
    }

    @Transactional
    public ConversationMessage chat(UUID interviewId, String userMessage) {
        if (!aiPolicyProvider.isAiEnabled(interviewId)) {
            throw new AiDisabledException("此階段 AI 不可用，請獨立完成題目");
        }
        if (interviewTimeProvider.isExpired(interviewId)) {
            throw new InterviewExpiredException("面試時間已到，無法使用 AI");
        }

        String conversationId = interviewId.toString();
        eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, MessageType.USER, userMessage));

        Optional<ChatModel> chatModelOpt = resolveChatModel(resolveModelId(interviewId, null));
        if (chatModelOpt.isEmpty()) {
            String stub = "AI assistant is not configured for this environment. " +
                    "Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.";
            repository.save(ConversationMessage.create(interviewId, MessageType.USER, userMessage));
            repository.save(ConversationMessage.create(interviewId, MessageType.ASSISTANT, stub));
            List<ConversationMessage> all = repository.findByInterviewIdOrderByCreatedAtAsc(interviewId);
            return all.getLast();
        }

        Map<String, Object> toolContextMap = Map.of("interviewId", interviewId.toString());
        org.springframework.ai.chat.model.ChatResponse aiResponse =
                callWithToolLoop(chatModelOpt.get(), conversationId, userMessage, toolContextMap);

        String response = aiResponse.getResult().getOutput().getText();
        updateLastAssistantTokenUsage(interviewId, aiResponse);

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
        Optional<ChatModel> chatModelOpt = resolveChatModel(resolvedModelId);

        // 發布 USER 訊息事件（DB 寫入由 callWithToolLoop 內的 chatMemory.add() 完成）
        try {
            eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, MessageType.USER, userMessage));
        } catch (Exception e) {
            log.error("Failed to publish user message event for interview {}", interviewId, e);
        }

        if (chatModelOpt.isEmpty()) {
            String stub = "AI assistant is not configured for this environment. " +
                    "Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.";
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

        // 設計說明：Flux.defer() 包裝 callWithToolLoop()（阻塞 API）。
        // 不使用 Flux.timeout()，因為 tool loop 期間 Flux 不發出任何 item，
        // 90s timeout 必然在 tool loop 未結束前觸發 TimeoutException。
        // SSE 保活由兩層機制負責：
        //   1. tool 事件（toolSseEmitter）：每個工具執行前後推送 SSE，同時維持 HTTP 連線
        //   2. heartbeat（15s SSE comment）：在 model call 無 tool 事件期間維持連線
        // Controller 的 latch.await(330s) 作為最終兜底安全閥。
        StringBuilder fullResponse = new StringBuilder();
        final ChatModel chatModel = chatModelOpt.get();

        Flux<String> stream = Flux.defer(() -> {
                    long t0 = System.currentTimeMillis();
                    log.info("[AI-DIAG] interview={} .call() starting (model={})", interviewId, resolvedModelId);

                    org.springframework.ai.chat.model.ChatResponse aiResponse =
                            callWithToolLoop(chatModel, conversationId, userMessage, toolContextMap);

                    String fullContent = aiResponse.getResult().getOutput().getText();
                    updateLastAssistantTokenUsage(interviewId, aiResponse);

                    long elapsed = System.currentTimeMillis() - t0;
                    log.info("[AI-DIAG] interview={} .call() completed in {}ms, content={}",
                            interviewId, elapsed,
                            fullContent == null ? "null" : "length=" + fullContent.length());

                    if (fullContent == null || fullContent.isBlank()) {
                        log.warn("[AI-DIAG] interview={} empty content after tool calling", interviewId);
                        fullContent = "（AI 已完成工具分析，但未產生文字回覆。請再試一次或換個方式提問。）";
                    }

                    List<String> chunks = splitIntoChunks(fullContent, 20);
                    log.debug("[AI-DIAG] interview={} split into {} chunks", interviewId, chunks.size());
                    return Flux.fromIterable(chunks);
                })
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

    /**
     * 設計說明：User Controlled Tool Execution — 手動 tool loop + 手動 ChatMemory 管理。
     *
     * Flow：
     * 1. 存 user message → chatMemory（持久化到 DB）
     * 2. 從 chatMemory 取完整歷史建立 Prompt（含 system prompt + tool options）
     * 3. 呼叫 chatModel.call(prompt)
     * 4. 存 ASSISTANT 回應（可能含 tool_calls）→ chatMemory
     * 5. 若有 tool calls：執行工具 → 存 ToolResponseMessage → 重新建立 Prompt → 再呼叫 model
     * 6. 重複直到 no more tool calls
     * 7. 回傳最終 ChatResponse（供外層取文字 + token usage）
     */
    private org.springframework.ai.chat.model.ChatResponse callWithToolLoop(
            ChatModel chatModel, String conversationId,
            String userMessage, Map<String, Object> toolContextMap) {

        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(workspaceTools))
                .toolContext(toolContextMap)
                .internalToolExecutionEnabled(false)
                .build();

        chatMemory.add(conversationId, List.of(new UserMessage(userMessage)));

        // 設計說明：使用 in-memory 訊息清單建構 prompt，而非每次迭代從 DB 重新載入。
        // 根本原因：Gemini thinking 模型的 AssistantMessage.properties() 含 thoughtSignatures（List<byte[]>），
        // 每次 DB round-trip 都會丟失這些 metadata（ConversationChatMemoryRepository 無法序列化 byte[]）。
        // 丟失 thoughtSignatures → 下次 API call 的 functionCall parts 缺少 thought_signature → 400 錯誤。
        // 解法：第一次呼叫後，以 in-memory list 累積所有訊息（含 metadata），取代 chatMemory.get() 重載。
        // chatMemory.add() 仍持久化到 DB，確保重啟安全；in-memory 只用於當次 request 的 prompt 建構。
        // 參考：https://ai.google.dev/gemini-api/docs/thought-signatures
        List<Message> inMemoryMessages = new ArrayList<>(chatMemory.get(conversationId));
        Prompt prompt = new Prompt(inMemoryMessages, options);

        // 第一次呼叫：若失敗（API key 無效、模型不存在等）直接拋出，DB 只有 USER 訊息，不需 fallback
        org.springframework.ai.chat.model.ChatResponse response;
        try {
            response = chatModel.call(prompt);
        } catch (Exception e) {
            log.error("[AI-DIAG] interview={} 1st chatModel.call() failed. Error: {}",
                    conversationId, e.getMessage(), e);
            throw new RuntimeException("AI 呼叫失敗：" + e.getMessage(), e);
        }
        AssistantMessage firstOutput = response.getResult().getOutput();
        chatMemory.add(conversationId, List.of(firstOutput));
        inMemoryMessages.add(firstOutput);

        int loopCount = 0;
        while (response.hasToolCalls()) {
            loopCount++;
            final int currentLoop = loopCount;
            // 診斷 log：記錄 tool calls 內容，確認是否缺少 thought_signature
            if (log.isDebugEnabled()) {
                response.getResult().getOutput().getToolCalls().forEach(tc ->
                        log.debug("[AI-DIAG] interview={} loop#{} toolCall id={} name={} args={}",
                                conversationId, currentLoop, tc.id(), tc.name(), tc.arguments()));
            }
            ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response);
            Message toolResponse = result.conversationHistory().getLast();
            // 1. 持久化到 DB（保證重啟安全）
            chatMemory.add(conversationId, List.of(toolResponse));
            // 2. 加入 in-memory list（保留原始 metadata 含 thoughtSignatures）
            inMemoryMessages.add(toolResponse);
            // 3. 用 in-memory 建構下次 prompt（非 chatMemory.get()，避免 DB round-trip 丟失 metadata）
            prompt = new Prompt(inMemoryMessages, options);

            // 診斷 log：記錄即將送出的 prompt 訊息清單（message type + toolCalls summary）
            log.info("[AI-DIAG] interview={} loop#{} prompt messages: {}", conversationId, currentLoop,
                    prompt.getInstructions().stream()
                            .map(m -> {
                                if (m instanceof AssistantMessage am && am.hasToolCalls()) {
                                    return "ASSISTANT(toolCalls=" + am.getToolCalls().stream()
                                            .map(tc -> tc.name() + "[id=" + tc.id() + "]")
                                            .toList() + ")";
                                }
                                return m.getMessageType() + "(len=" +
                                        (m.getText() == null ? 0 : m.getText().length()) + ")";
                            })
                            .toList());

            // 第二次（含）以後的呼叫：帶 tool response 結果要求 model 生成文字。
            // 部分 Preview 模型（如 gemini-3.1-flash-lite-preview）的 tool calling 支援可能不完整，
            // 或 Spring AI M2 的 Google GenAI 適配器格式不符 API 預期，導致此步驟失敗。
            // 發生錯誤時存入 fallback ASSISTANT 訊息，確保對話歷史保持完整
            // （ASSISTANT(toolCalls) + TOOL_RESPONSE 後必須有 ASSISTANT，否則下次對話無法正確接續）。
            try {
                response = chatModel.call(prompt);
            } catch (Exception e) {
                log.error("[AI-DIAG] interview={} tool loop call #{} failed after {} tool execution(s). Error: {}",
                        conversationId, loopCount + 1, loopCount, e.getMessage(), e);
                String fallbackText = "（AI 工具分析完成，但生成回應時發生錯誤。請再試一次或換個模型。）";
                chatMemory.add(conversationId, List.of(new AssistantMessage(fallbackText)));
                throw new RuntimeException("AI 回應失敗：" + e.getMessage(), e);
            }
            AssistantMessage loopOutput = response.getResult().getOutput();
            chatMemory.add(conversationId, List.of(loopOutput));
            inMemoryMessages.add(loopOutput);
        }

        return response;
    }

    private String resolveModelId(UUID interviewId, String modelIdOverride) {
        if (modelIdOverride != null && !modelIdOverride.isBlank()) {
            return modelIdOverride;
        }
        return interviewModelProvider.getAiModel(interviewId);
    }

    private Optional<ChatModel> resolveChatModel(String modelId) {
        Optional<ChatModel> model = modelRegistry.getChatModel(modelId);
        if (model.isEmpty()) {
            log.warn("Model '{}' not found in registry, trying first available", modelId);
            return modelRegistry.getFirstChatModel();
        }
        return model;
    }

    /**
     * 設計說明：Post-update 策略 — advisor chain 已將 ASSISTANT 訊息存入 DB，
     * 但 token usage 只能從最終 ChatResponse metadata 取得。
     * 因此在 AI 呼叫完成後，回填最後一筆 ASSISTANT 訊息的 token 欄位。
     * try-catch 確保 token 追蹤失敗不影響聊天流程。
     */
    private void updateLastAssistantTokenUsage(UUID interviewId,
            org.springframework.ai.chat.model.ChatResponse chatResponse) {
        try {
            var metadata = chatResponse.getMetadata();
            var usage = metadata.getUsage();
            String model = metadata.getModel();
            Integer promptTokens = usage != null ? usage.getPromptTokens() : null;
            Integer completionTokens = usage != null ? usage.getCompletionTokens() : null;

            repository.findLastAssistantMessage(interviewId).ifPresent(msg -> {
                msg.updateTokenUsage(promptTokens, completionTokens, model);
                repository.save(msg);
            });
        } catch (Exception e) {
            log.warn("Failed to update token usage for interview {}", interviewId, e);
        }
    }

}
