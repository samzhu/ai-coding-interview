package com.interview.ai.application;

import com.interview.ai.AiChatMessageEvent;
import com.interview.ai.domain.ConversationMessage;
import com.interview.ai.infrastructure.persistence.ConversationMessageRepository;
import com.interview.ai.internal.AiModelRegistry;
import com.interview.ai.internal.CrossModelChatMemory;
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
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;


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
 *
 * action: 前綴設計：
 * 候選人 accept/reject editProposal 時，前端呼叫 sendMessage("action:ACCEPTED") 或
 * sendMessage("action:REJECTED")，透過原本的 /chat/stream 端點傳入。
 * streamChat() 偵測 action: 前綴後 strip 前綴，以純 "ACCEPTED"/"REJECTED" 當一般
 * user message 處理，不發布 USER 事件（action 不是真正的使用者訊息）。
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
    // AI 工具集（listFiles、readFile、runCommand、editProposal），透過 tool calling 讓 AI 自動呼叫
    private final InterviewWorkspaceTools workspaceTools;
    private final ChatMemory chatMemory;

    public AiChatService(ConversationMessageRepository repository,
                         AiModelRegistry modelRegistry,
                         InterviewModelProvider interviewModelProvider,
                         InterviewAiPolicyProvider aiPolicyProvider,
                         InterviewTimeProvider interviewTimeProvider,
                         ApplicationEventPublisher eventPublisher,
                         InterviewWorkspaceTools workspaceTools,
                         ChatMemory chatMemory) {
        this.repository = repository;
        this.modelRegistry = modelRegistry;
        this.interviewModelProvider = interviewModelProvider;
        this.aiPolicyProvider = aiPolicyProvider;
        this.interviewTimeProvider = interviewTimeProvider;
        this.eventPublisher = eventPublisher;
        this.workspaceTools = workspaceTools;
        this.chatMemory = chatMemory;
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
                callWithToolLoop(chatModelOpt.get(), conversationId, userMessage, toolContextMap,
                        resolveModelId(interviewId, null));

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

        // 設計說明：Accept/Reject 透過原本的 /chat/stream 送入，用 action: 前綴區分。
        // Strip 前綴後當一般 user message 處理（LLM 的 system prompt 已告知會收到 ACCEPTED/REJECTED）。
        // 不發布 AiChatMessageEvent USER 事件（action 不是真正的使用者訊息，前端也不渲染）。
        final String effectiveUserMessage;
        final boolean isAction = userMessage.startsWith("action:");
        if (isAction) {
            effectiveUserMessage = userMessage.substring("action:".length()); // "ACCEPTED" or "REJECTED"
            log.info("[AI-DIAG] interview={} action message detected: {}", interviewId, effectiveUserMessage);
        } else {
            effectiveUserMessage = userMessage;
            // 發布 USER 訊息事件（DB 寫入由 callWithToolLoop 內的 chatMemory.add() 完成）
            try {
                eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, MessageType.USER, userMessage));
            } catch (Exception e) {
                log.error("Failed to publish user message event for interview {}", interviewId, e);
            }
        }

        if (chatModelOpt.isEmpty()) {
            String stub = "AI assistant is not configured for this environment. " +
                    "Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.";
            try {
                repository.save(ConversationMessage.create(interviewId, MessageType.USER, effectiveUserMessage));
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
        // Controller 的 latch.await(5min) 作為最終兜底安全閥。
        StringBuilder fullResponse = new StringBuilder();
        final ChatModel chatModel = chatModelOpt.get();

        Flux<String> stream = Flux.defer(() -> {
                    long t0 = System.currentTimeMillis();
                    log.info("[AI-DIAG] interview={} .call() starting (model={}, isAction={})",
                            interviewId, resolvedModelId, isAction);

                    org.springframework.ai.chat.model.ChatResponse aiResponse =
                            callWithToolLoop(chatModel, conversationId, effectiveUserMessage, toolContextMap,
                                    resolvedModelId);

                    String fullContent = aiResponse.getResult().getOutput().getText();

                    long elapsed = System.currentTimeMillis() - t0;
                    log.info("[AI-DIAG] interview={} .call() completed in {}ms, content={}",
                            interviewId, elapsed,
                            fullContent == null ? "null" : "length=" + fullContent.length());

                    updateLastAssistantTokenUsage(interviewId, aiResponse);

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
     * 為何自行執行 tool calls（而非 ToolCallingManager）：
     * ToolCallingManager.executeToolCalls() 是黑盒，可能 mutate AssistantMessage 物件，
     * 導致後續 prompt 中 tool_result 找不到對應的 tool_use（Anthropic API 400 錯誤）。
     * 改為自行建立 ToolCallback map → 執行 → 建構 ToolResponseMessage，完全掌控 ID 配對。
     * 參考：https://docs.spring.io/spring-ai/reference/2.0/api/tools.html（User Controlled Tool Execution）
     *
     * 此方法建立 CrossModelChatMemory、存入 userMessage，然後呼叫 callLLMAndLoop()。
     */
    private org.springframework.ai.chat.model.ChatResponse callWithToolLoop(
            ChatModel chatModel, String conversationId,
            String userMessage, Map<String, Object> toolContextMap, String resolvedModelId) {

        final CrossModelChatMemory memory = new CrossModelChatMemory(chatMemory, modelRegistry, resolvedModelId);
        memory.add(conversationId, List.of(new UserMessage(userMessage)));
        return callLLMAndLoop(chatModel, memory, conversationId, toolContextMap, resolvedModelId);
    }

    /**
     * 設計說明：從當前 memory 狀態繼續 LLM 對話的核心方法。
     *
     * 載入歷史 → chatModel.call() → tool loop（editProposal 正常執行，不中斷）。
     *
     * Flow：
     * 1. 從 memory 取完整歷史建立 Prompt（含 system prompt + tool options）
     * 2. 呼叫 chatModel.call(prompt)
     * 3. 存 ASSISTANT 回應（可能含 tool_calls）→ memory
     * 4. 若有 tool calls：逐一執行 → 存 TOOL responses → 重新建立 Prompt → 再呼叫 model
     * 5. 重複直到 no more tool calls
     * 6. 回傳最終 ChatResponse
     */
    private org.springframework.ai.chat.model.ChatResponse callLLMAndLoop(
            ChatModel chatModel, CrossModelChatMemory memory,
            String conversationId, Map<String, Object> toolContextMap, String resolvedModelId) {

        // 建立 ToolCallback map，供 while loop 內逐一查找並執行
        ToolCallback[] callbacks = ToolCallbacks.from(workspaceTools);
        Map<String, ToolCallback> callbackMap = Arrays.stream(callbacks)
                .collect(Collectors.toMap(cb -> cb.getToolDefinition().name(), cb -> cb));
        ToolContext toolContext = new ToolContext(toolContextMap);

        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(callbacks)
                .toolContext(toolContextMap)
                .internalToolExecutionEnabled(false)
                .build();

        // 從 DB 讀取歷史（含 orphan cleanup + provider 適配）
        List<Message> messages = memory.get(conversationId);

        // 防禦性驗證：適配後最後一條訊息應為 USER（對 callWithToolLoop 路徑）
        if (!messages.isEmpty() && !(messages.getLast() instanceof UserMessage)) {
            log.debug("[AI-DIAG] interview={} callLLMAndLoop: last message type={}",
                    conversationId, messages.getLast().getMessageType());
        }

        Prompt prompt = new Prompt(messages, options);

        // 第一次呼叫：若失敗且歷史含 tool call，嘗試降級為文字摺疊後重試
        org.springframework.ai.chat.model.ChatResponse response;
        try {
            response = chatModel.call(prompt);
        } catch (Exception e) {
            log.error("[AI-DIAG] interview={} 1st chatModel.call() failed. Error: {}",
                    conversationId, e.getMessage(), e);
            boolean hasToolHistory = messages.stream()
                    .anyMatch(m -> m instanceof AssistantMessage am && am.hasToolCalls());
            if (hasToolHistory) {
                log.warn("[AI-DIAG] interview={} retrying with folded tool history", conversationId);
                prompt = new Prompt(new ArrayList<>(memory.getFolded(conversationId)), options);
                response = chatModel.call(prompt);
            } else {
                throw new RuntimeException("AI 呼叫失敗：" + e.getMessage(), e);
            }
        }

        // 解析並持久化第一次 ASSISTANT 回應
        // 設計說明：resolveAssistantOutput() 掃描所有 Generation，將文字 + tool calls
        // 合併為一個 AssistantMessage。這是因為部分 provider（如 Anthropic/Claude）在
        // Spring AI 2.0.0-M2 中可能將文字 content block 和 tool_use block 拆成多個
        // Generation，導致 response.getResult()（只取第一個）拿不到 tool calls，
        // 而 response.hasToolCalls()（掃描所有）返回 true，產生不一致。
        AssistantMessage assistantOutput = resolveAssistantOutput(response, conversationId);
        memory.add(conversationId, List.of(assistantOutput));

        // Tool loop — 以 assistantOutput.getToolCalls() 為準，有 tool calls 就執行
        int loopCount = 0;
        while (!assistantOutput.getToolCalls().isEmpty()) {
            loopCount++;
            final int currentLoop = loopCount;

            if (log.isDebugEnabled()) {
                assistantOutput.getToolCalls().forEach(tc ->
                        log.debug("[AI-DIAG] interview={} loop#{} toolCall id={} name={} args={}",
                                conversationId, currentLoop, tc.id(), tc.name(), tc.arguments()));
            }

            // 執行 tool calls：逐一查找 ToolCallback → 執行 → 收集 ToolResponse
            List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
            for (AssistantMessage.ToolCall tc : assistantOutput.getToolCalls()) {
                ToolCallback cb = callbackMap.get(tc.name());
                if (cb == null) {
                    log.warn("[AI-DIAG] interview={} loop#{} unknown tool '{}'", conversationId, currentLoop, tc.name());
                    toolResponses.add(new ToolResponseMessage.ToolResponse(
                            tc.id(), tc.name(), "Error: unknown tool '" + tc.name() + "'"));
                    continue;
                }
                String callResult = cb.call(tc.arguments(), toolContext);
                toolResponses.add(new ToolResponseMessage.ToolResponse(
                        tc.id(), tc.name(), callResult != null ? callResult : ""));
            }

            memory.add(conversationId, List.of(ToolResponseMessage.builder().responses(toolResponses).build()));

            // 從 DB 重新讀取完整歷史建構 prompt（含 orphan cleanup + provider 適配）
            messages = memory.get(conversationId);
            prompt = new Prompt(messages, options);

            log.info("[AI-DIAG] interview={} loop#{} prompt messages: {}", conversationId, currentLoop,
                    messages.stream()
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

            // 呼叫 model 取得下一輪回應
            // 發生錯誤時存入 fallback ASSISTANT，確保 ASSISTANT(toolCalls)+TOOL 後一定有 ASSISTANT
            try {
                response = chatModel.call(prompt);
            } catch (Exception e) {
                log.error("[AI-DIAG] interview={} tool loop call #{} failed. Error: {}",
                        conversationId, currentLoop + 1, e.getMessage(), e);
                String fallbackText = "（AI 工具分析完成，但生成回應時發生錯誤。請再試一次或換個模型。）";
                memory.add(conversationId, List.of(new AssistantMessage(fallbackText)));
                throw new RuntimeException("AI 回應失敗：" + e.getMessage(), e);
            }

            assistantOutput = resolveAssistantOutput(response, conversationId);
            memory.add(conversationId, List.of(assistantOutput));
        }

        return response;
    }

    /**
     * 設計說明：從 ChatResponse 中取出含 tool calls 的 AssistantMessage。
     *
     * 部分 AI provider（如 Anthropic/Claude）在 Spring AI 2.0.0-M2 中可能將
     * 文字 content block 和 tool_use block 分拆為多個 Generation：
     *   Generation[0]：純文字（response.getResult() 預設回傳此項）
     *   Generation[1]：tool calls（response.hasToolCalls() 掃描到此項）
     * 此方法掃描全部 results，將文字 + tool calls 合併為一個 AssistantMessage，
     * 確保 memory 中 ASSISTANT(toolCalls) + TOOL 可以正確成對。
     * 若無 tool calls，直接回傳第一個 Generation 的 output（純文字回應）。
     */
    private AssistantMessage resolveAssistantOutput(
            org.springframework.ai.chat.model.ChatResponse response, String conversationId) {
        List<Generation> results = response.getResults();

        // 單一 Generation — 直接回傳原始 output（保留 thoughtSignatures 等 provider metadata）
        if (results.size() == 1) {
            return results.getFirst().getOutput();
        }

        // 多 Generation — 掃描合併（Claude 將 text + tool_use 拆成多個 Generation）
        List<AssistantMessage.ToolCall> allToolCalls = results.stream()
                .filter(g -> g.getOutput() != null && g.getOutput().hasToolCalls())
                .flatMap(g -> g.getOutput().getToolCalls().stream())
                .toList();

        if (!allToolCalls.isEmpty()) {
            String textContent = results.stream()
                    .map(g -> g.getOutput().getText())
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.joining("\n"));
            // 合併 metadata（以含 tool calls 的 Generation 為主），保留 thoughtSignatures 等
            Map<String, Object> mergedProps = new HashMap<>();
            results.stream()
                    .map(g -> g.getOutput().getMetadata())
                    .forEach(mergedProps::putAll);
            log.debug("[AI-DIAG] interview={} resolveAssistantOutput: {} result(s) → merged (text={}chars, toolCalls={})",
                    conversationId, results.size(), textContent.length(),
                    allToolCalls.stream().map(AssistantMessage.ToolCall::name).toList());
            return AssistantMessage.builder()
                    .content(textContent)
                    .toolCalls(allToolCalls)
                    .properties(mergedProps)
                    .build();
        }

        // 多 Generation 但無 tool calls — 回傳第一個
        return response.getResult().getOutput();
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
