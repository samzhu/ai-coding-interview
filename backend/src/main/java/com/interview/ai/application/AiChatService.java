package com.interview.ai.application;

import com.interview.ai.AiChatMessageEvent;
import com.interview.ai.domain.ConversationMessage;
import com.interview.ai.infrastructure.persistence.ConversationMessageRepository;
import com.interview.ai.internal.AiModelRegistry;
import com.interview.ai.internal.CrossModelChatMemory;
import com.interview.ai.internal.InterviewWorkspaceTools;
import com.interview.interview.InterviewAiPolicyProvider;
import com.interview.interview.InterviewExpiredException;
import com.interview.interview.InterviewFileProvider;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.ai.chat.model.ChatResponse;
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
 * 候選人 accept/reject editProposal 時，前端呼叫 sendMessage("action:ACCEPTED:<proposalId>") 或
 * sendMessage("action:REJECTED:<proposalId>")，透過原本的 /chat/stream 端點傳入。
 * streamChat() 偵測 action: 前綴後，ACCEPTED/REJECTED 不呼叫 LLM，
 * 直接寫入固定回應到 memory，節省 5-30 秒等待時間和 token。
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
    // 用於 handleAccepted()：寫回後端檔案 + 解析 tool call arguments JSON
    private final InterviewFileProvider fileProvider;
    private final ObjectMapper objectMapper;

    public AiChatService(ConversationMessageRepository repository,
                         AiModelRegistry modelRegistry,
                         InterviewModelProvider interviewModelProvider,
                         InterviewAiPolicyProvider aiPolicyProvider,
                         InterviewTimeProvider interviewTimeProvider,
                         ApplicationEventPublisher eventPublisher,
                         InterviewWorkspaceTools workspaceTools,
                         ChatMemory chatMemory,
                         InterviewFileProvider fileProvider,
                         ObjectMapper objectMapper) {
        this.repository = repository;
        this.modelRegistry = modelRegistry;
        this.interviewModelProvider = interviewModelProvider;
        this.aiPolicyProvider = aiPolicyProvider;
        this.interviewTimeProvider = interviewTimeProvider;
        this.eventPublisher = eventPublisher;
        this.workspaceTools = workspaceTools;
        this.chatMemory = chatMemory;
        this.fileProvider = fileProvider;
        this.objectMapper = objectMapper;
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
                        resolveModelId(interviewId, null), false);

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

        // 設計說明：action: 前綴用於候選人 accept/reject editProposal。
        // 新格式 "action:ACCEPTED:<proposalId>" / "action:REJECTED:<proposalId>"。
        // action 不是真正的使用者訊息，不發布 USER 事件。
        final boolean isAction = userMessage.startsWith("action:");
        final String effectiveUserMessage;
        if (isAction) {
            effectiveUserMessage = userMessage.substring("action:".length()); // "ACCEPTED:<id>" or "REJECTED:<id>"
            log.info("[AI] interview={} action detected: {}", interviewId, effectiveUserMessage);

            // 設計說明：ACCEPTED/REJECTED action 不需呼叫 LLM。
            // 後端直接寫入固定回應到 memory，節省 5-30 秒等待時間和 token 費用。
            if (effectiveUserMessage.startsWith("ACCEPTED:")) {
                String proposalId = effectiveUserMessage.substring("ACCEPTED:".length());
                return handleAccepted(interviewId, conversationId, proposalId, assistantMsgId);
            } else if (effectiveUserMessage.startsWith("REJECTED:")) {
                String proposalId = effectiveUserMessage.substring("REJECTED:".length());
                return handleRejected(interviewId, conversationId, proposalId, assistantMsgId);
            }
            // 未知 action 格式 — 以 effectiveUserMessage（無 action: 前綴）繼續 LLM 處理
            log.warn("[AI] interview={} unknown action format, falling through to LLM: {}", interviewId, effectiveUserMessage);
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
        final boolean isActionFallback = isAction; // 傳入 callWithToolLoop 停用工具（未知 action 格式 fallback）

        Flux<String> stream = Flux.defer(() -> {
                    long t0 = System.currentTimeMillis();
                    log.info("[AI] interview={} .call() starting (model={}, isAction={})",
                            interviewId, resolvedModelId, isActionFallback);

                    org.springframework.ai.chat.model.ChatResponse aiResponse =
                            callWithToolLoop(chatModel, conversationId, effectiveUserMessage, toolContextMap,
                                    resolvedModelId, isActionFallback);

                    String fullContent = aiResponse.getResult().getOutput().getText();

                    long elapsed = System.currentTimeMillis() - t0;
                    log.info("[AI] interview={} .call() completed in {}ms, content={}",
                            interviewId, elapsed,
                            fullContent == null ? "null" : "length=" + fullContent.length());

                    updateLastAssistantTokenUsage(interviewId, aiResponse);

                    if (fullContent == null || fullContent.isBlank()) {
                        log.warn("[AI] interview={} empty content after tool calling", interviewId);
                        fullContent = "（AI 已完成工具分析，但未產生文字回覆。請再試一次或換個方式提問。）";
                    }

                    List<String> chunks = splitIntoChunks(fullContent, 20);
                    log.debug("[AI] interview={} split into {} chunks", interviewId, chunks.size());
                    return Flux.fromIterable(chunks);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    try {
                        log.info("[AI] interview={} assistant sent, length={}", interviewId, fullResponse.length());
                        eventPublisher.publishEvent(new AiChatMessageEvent(
                                interviewId, MessageType.ASSISTANT, fullResponse.toString()));
                    } catch (Exception e) {
                        log.error("Failed to publish assistant message event after streaming", e);
                    }
                })
                .doOnError(e -> log.error("AI stream failed for interview {} (model={})", interviewId, resolvedModelId, e));

        return new StreamingChatResult(stream, assistantMsgId);
    }

    /**
     * 設計說明：候選人按同意後，不呼叫 LLM，直接：
     * 1. 寫入 UserMessage("ACCEPTED") 到 chatMemory（完整對話紀錄）
     * 2. 掃描 chatMemory 歷史，找到 proposalId 對應的 editProposal toolCall
     * 3. 解析 FileChange 並逐一呼叫 fileProvider.writeFile() 寫回後端
     * 4. 寫入固定 AssistantMessage 到 chatMemory
     * 5. 發布 ASSISTANT 事件，回傳固定文字串流
     *
     * proposalId 比對策略：
     * - 精確比對 tc.id() == proposalId（Claude/OpenAI 有唯一 ID）
     * - 退而求次：找最後一個 name == "editProposal" 的 toolCall（Gemini name-based 配對）
     */
    private StreamingChatResult handleAccepted(UUID interviewId, String conversationId,
                                               String proposalId, UUID assistantMsgId) {
        StringBuilder fullResponse = new StringBuilder();
        Flux<String> stream = Flux.defer(() -> {
            // 設計說明：直接使用底層 chatMemory（非 CrossModelChatMemory），
            // 純文字 USER/ASSISTANT 訊息不需 provider 格式轉換。
            chatMemory.add(conversationId, List.of(new UserMessage("ACCEPTED")));

            // 掃描 memory 找 proposalId 對應的 editProposal tool call arguments
            List<Message> history = chatMemory.get(conversationId);
            String tcArgs = null;
            String fallbackArgs = null;
            outer:
            for (int i = history.size() - 1; i >= 0; i--) {
                Message msg = history.get(i);
                if (msg instanceof AssistantMessage am && am.hasToolCalls()) {
                    for (AssistantMessage.ToolCall tc : am.getToolCalls()) {
                        if ("editProposal".equals(tc.name())) {
                            if (proposalId.equals(tc.id())) {
                                tcArgs = tc.arguments();
                                break outer;
                            }
                            // 保留最後一個 editProposal 作為 fallback（Gemini name-based 配對）
                            if (fallbackArgs == null) {
                                fallbackArgs = tc.arguments();
                            }
                        }
                    }
                }
            }
            if (tcArgs == null) {
                tcArgs = fallbackArgs;
                log.warn("[AI] interview={} handleAccepted: proposalId={} not matched by id, using fallback",
                        interviewId, proposalId);
            }

            // 解析 FileChange 並寫檔
            int filesWritten = 0;
            if (tcArgs != null) {
                try {
                    JsonNode root = objectMapper.readTree(tcArgs);
                    JsonNode changesNode = root.get("changes");
                    if (changesNode != null && changesNode.isArray()) {
                        for (JsonNode changeNode : changesNode) {
                            String filePath = changeNode.get("file").asText();
                            String proposed = changeNode.get("proposed").asText();
                            fileProvider.writeFile(interviewId, filePath, proposed);
                            filesWritten++;
                            log.info("[AI] interview={} handleAccepted: wrote file={}", interviewId, filePath);
                        }
                    }
                } catch (Exception e) {
                    log.warn("[AI] interview={} handleAccepted: failed to parse/write files, proposalId={}",
                            interviewId, proposalId, e);
                }
            } else {
                log.warn("[AI] interview={} handleAccepted: no editProposal toolCall found in history, proposalId={}",
                        interviewId, proposalId);
            }

            String responseText = "已套用 " + filesWritten + " 個檔案修改。";
            chatMemory.add(conversationId, List.of(new AssistantMessage(responseText)));
            return Flux.just(responseText);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnNext(fullResponse::append)
        .doOnComplete(() -> {
            try {
                eventPublisher.publishEvent(new AiChatMessageEvent(
                        interviewId, MessageType.ASSISTANT, fullResponse.toString()));
            } catch (Exception e) {
                log.error("Failed to publish assistant message event for handleAccepted interview {}", interviewId, e);
            }
        })
        .doOnError(e -> log.error("handleAccepted failed for interview={} proposalId={}", interviewId, proposalId, e));

        return new StreamingChatResult(stream, assistantMsgId);
    }

    /**
     * 設計說明：候選人按拒絕後，不呼叫 LLM，直接：
     * 1. 寫入 UserMessage("REJECTED") 到 chatMemory
     * 2. 寫入固定 AssistantMessage 到 chatMemory
     * 3. 發布 ASSISTANT 事件，回傳固定文字串流
     */
    private StreamingChatResult handleRejected(UUID interviewId, String conversationId,
                                               String proposalId, UUID assistantMsgId) {
        StringBuilder fullResponse = new StringBuilder();
        Flux<String> stream = Flux.defer(() -> {
            chatMemory.add(conversationId, List.of(new UserMessage("REJECTED")));
            String responseText = "了解，保留原始程式碼。";
            chatMemory.add(conversationId, List.of(new AssistantMessage(responseText)));
            log.info("[AI] interview={} handleRejected: proposalId={}", interviewId, proposalId);
            return Flux.just(responseText);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnNext(fullResponse::append)
        .doOnComplete(() -> {
            try {
                eventPublisher.publishEvent(new AiChatMessageEvent(
                        interviewId, MessageType.ASSISTANT, fullResponse.toString()));
            } catch (Exception e) {
                log.error("Failed to publish assistant message event for handleRejected interview {}", interviewId, e);
            }
        })
        .doOnError(e -> log.error("handleRejected failed for interview={} proposalId={}", interviewId, proposalId, e));

        return new StreamingChatResult(stream, assistantMsgId);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> getHistory(UUID interviewId) {
        // 直接查詢 repository 而非透過 ChatMemory，因為 API 回傳需要 id、createdAt 等 DB 欄位
        return repository.findByInterviewIdOrderByCreatedAtAsc(interviewId);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max) + "...";
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
            String userMessage, Map<String, Object> toolContextMap, String resolvedModelId, boolean isAction) {

        final CrossModelChatMemory memory = new CrossModelChatMemory(chatMemory, modelRegistry, resolvedModelId);
        memory.add(conversationId, List.of(new UserMessage(userMessage)));
        return callLLMAndLoop(chatModel, memory, conversationId, toolContextMap, resolvedModelId, isAction);
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
            String conversationId, Map<String, Object> toolContextMap, String resolvedModelId, boolean isAction) {

        // 建立 ToolCallback map，供 while loop 內逐一查找並執行
        ToolCallback[] callbacks = ToolCallbacks.from(workspaceTools);

        // 設計說明：action:ACCEPTED:<proposalId> / action:REJECTED:<proposalId> 已在 streamChat()
        // 提前攔截，由 handleAccepted/handleRejected 直接處理，不會走到 callLLMAndLoop()。
        // 此 isAction=true 分支目前為**未知 action 格式**的 fallback，
        // 例如前端誤送 "action:UNKNOWN"，此時不提供任何工具，僅讓 LLM 回應文字說明。
        if (isAction) {
            callbacks = new ToolCallback[0];
        }
        Map<String, ToolCallback> callbackMap = Arrays.stream(callbacks)
                .collect(Collectors.toMap(cb -> cb.getToolDefinition().name(), cb -> cb));

        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(callbacks)
                .toolContext(toolContextMap)
                .internalToolExecutionEnabled(false)
                .build();

        // 從 DB 讀取歷史（含 orphan cleanup + provider 適配）
        List<Message> messages = memory.get(conversationId);

        // 防禦性驗證：適配後最後一條訊息應為 USER（對 callWithToolLoop 路徑）
        if (!messages.isEmpty() && !(messages.getLast() instanceof UserMessage)) {
            log.debug("[AI] interview={} callLLMAndLoop: last message type={}",
                    conversationId, messages.getLast().getMessageType());
        }

        Prompt prompt = new Prompt(messages, options);

        // 第一次呼叫：若失敗且歷史含 tool call，嘗試降級為文字摺疊後重試
        org.springframework.ai.chat.model.ChatResponse response;
        try {
            response = chatModel.call(prompt);
        } catch (Exception e) {
            log.error("[AI] interview={} 1st chatModel.call() failed. Error: {}",
                    conversationId, e.getMessage(), e);
            boolean hasToolHistory = messages.stream()
                    .anyMatch(m -> m instanceof AssistantMessage am && am.hasToolCalls());
            if (hasToolHistory) {
                log.warn("[AI] interview={} retrying with folded tool history", conversationId);
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
        log.info("[AI] loop#0 LLM → text={}chars tools={} preview={}",
                assistantOutput.getText() == null ? 0 : assistantOutput.getText().length(),
                assistantOutput.getToolCalls().stream().map(AssistantMessage.ToolCall::name).toList(),
                truncate(assistantOutput.getText(), 200));
        memory.add(conversationId, List.of(assistantOutput));

        // Tool loop — 以 assistantOutput.getToolCalls() 為準，有 tool calls 就執行
        int loopCount = 0;
        while (!assistantOutput.getToolCalls().isEmpty()) {
            loopCount++;
            final int currentLoop = loopCount;

                // 執行 tool calls：逐一查找 ToolCallback → 執行 → 收集 ToolResponse
            List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
            for (AssistantMessage.ToolCall tc : assistantOutput.getToolCalls()) {
                ToolCallback cb = callbackMap.get(tc.name());
                if (cb == null) {
                    log.warn("[AI] loop#{} unknown tool '{}'", currentLoop, tc.name());
                    // TODO: remove this workaround after spring-ai PR #5438 is merged
                    // Gemini FunctionResponse requires a JSON object; plain text causes parseJsonToMap() to crash
                    String availableTools = String.join(", ", callbackMap.keySet());
                    toolResponses.add(new ToolResponseMessage.ToolResponse(
                            tc.id(), tc.name(),
                            "{\"error\":\"unknown tool '" + tc.name() + "'. Available tools: " + availableTools + "\"}"));
                    continue;
                }
                // 設計說明：每次 tool call 建立獨立的 ToolContext，注入 currentToolCallId（= ToolCall.id）。
                // editProposal 工具從 ToolContext 讀取此 ID 當作 proposalId，寫入 SSE 事件供前端精確追蹤。
                // 使用 per-call HashMap 而非修改共用 toolContextMap，避免多 tool call 時 ID 相互覆蓋。
                Map<String, Object> perCallCtxMap = new HashMap<>(toolContextMap);
                perCallCtxMap.put("currentToolCallId", tc.id());
                ToolContext perCallContext = new ToolContext(perCallCtxMap);
                long t0 = System.currentTimeMillis();
                String callResult = cb.call(tc.arguments(), perCallContext);
                long elapsed = System.currentTimeMillis() - t0;
                log.info("[AI] loop#{} TOOL {} args={} → {}chars ({}ms) result={}",
                        currentLoop, tc.name(), truncate(tc.arguments(), 120),
                        callResult != null ? callResult.length() : 0, elapsed,
                        truncate(callResult, 80));
                // TODO: remove this workaround after spring-ai PR #5438 is merged
                // Gemini FunctionResponse requires a JSON object; empty string causes parseJsonToMap() to crash
                toolResponses.add(new ToolResponseMessage.ToolResponse(
                        tc.id(), tc.name(), callResult != null ? callResult : "{\"result\":\"ok\"}"));
            }

            memory.add(conversationId, List.of(ToolResponseMessage.builder().responses(toolResponses).build()));

            // 設計說明：editProposal 工具執行時已透過 SSE 將 diff 發送到前端，
            // ToolResponse 寫入 memory 後即可結束 tool loop，等待候選人回應。
            // 不需要再呼叫 LLM — 候選人已在前端看到 diff，等待其 ACCEPTED/REJECTED。
            boolean hasEditProposal = assistantOutput.getToolCalls().stream()
                    .anyMatch(tc -> "editProposal".equals(tc.name()));
            if (hasEditProposal) {
                log.info("[AI] loop#{} editProposal detected → break, waiting for candidate response", currentLoop);
                response = new ChatResponse(List.of(new Generation(
                        new AssistantMessage("已提交修改建議，請檢視上方的程式碼變更。"))));
                break;
            }

            // 從 DB 重新讀取完整歷史建構 prompt（含 orphan cleanup + provider 適配）
            messages = memory.get(conversationId);
            prompt = new Prompt(messages, options);

            // 呼叫 model 取得下一輪回應
            // 發生錯誤時存入 fallback ASSISTANT，確保 ASSISTANT(toolCalls)+TOOL 後一定有 ASSISTANT
            try {
                response = chatModel.call(prompt);
            } catch (Exception e) {
                log.error("[AI] interview={} tool loop call #{} failed. Error: {}",
                        conversationId, currentLoop + 1, e.getMessage(), e);
                String fallbackText = "（AI 工具分析完成，但生成回應時發生錯誤。請再試一次或換個模型。）";
                memory.add(conversationId, List.of(new AssistantMessage(fallbackText)));
                throw new RuntimeException("AI 回應失敗：" + e.getMessage(), e);
            }

            assistantOutput = resolveAssistantOutput(response, conversationId);
            log.info("[AI] loop#{} LLM({}msgs) → text={}chars tools={} preview={}",
                    currentLoop, messages.size(),
                    assistantOutput.getText() == null ? 0 : assistantOutput.getText().length(),
                    assistantOutput.getToolCalls().stream().map(AssistantMessage.ToolCall::name).toList(),
                    truncate(assistantOutput.getText(), 200));
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
            log.debug("[AI] interview={} resolveAssistantOutput: {} result(s) → merged (text={}chars, toolCalls={})",
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
