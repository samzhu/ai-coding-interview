package com.interview.ai.infrastructure.persistence;

import com.interview.ai.domain.ConversationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 實作 Spring AI 的 ChatMemoryRepository 介面，底層使用既有 ai_conversations 表。
 *
 * 為什麼不直接用 JdbcChatMemoryRepository？
 * - 我們需要額外欄位（model）和 FK 關聯（interview_id）
 * - JdbcChatMemoryRepository 使用自己的 SPRING_AI_CHAT_MEMORY 表，schema 不符需求
 * - JdbcChatMemoryRepository 官方實作對 TOOL 訊息只存空內容，丟失工具執行結果
 * - 透過 adapter pattern，我們保留既有 DB schema，同時符合 Spring AI 介面合約
 *
 * saveAll 採用「增量寫入」策略：
 * - 若新訊息比現有多：只插入新增的部分（保留現有訊息的 id/createdAt）
 * - 若新訊息比現有少：刪除最舊的訊息（MessageWindowChatMemory 的視窗截斷行為）
 * - 若數量相同：不做任何操作
 * 這個策略保留了現有訊息的 UUID 和 createdAt，避免前端 key 不穩定。
 *
 * TOOL 訊息序列化設計：
 * - AssistantMessage with toolCalls：序列化為 JSON 存 content（_type=assistant_tool_calls）
 * - ToolResponseMessage：序列化為 JSON 存 content（_type=tool_responses）
 * - 官方 JdbcChatMemoryRepository 此處只存 getText()，丟失 toolCalls/responses 資訊
 */
@Component
class ConversationChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger log = LoggerFactory.getLogger(ConversationChatMemoryRepository.class);

    private final ConversationMessageRepository repository;
    private final ObjectMapper objectMapper;

    ConversationChatMemoryRepository(ConversationMessageRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String> findConversationIds() {
        return repository.findDistinctInterviewIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        UUID interviewId = UUID.fromString(conversationId);
        // 設計說明：直接回傳 DB 原始訊息，不做任何 sanitization。
        // sanitization（摺疊 tool call 配對）已移至 AiChatService.callWithToolLoop()，
        // 在建構 Prompt 前執行。這樣做的根本原因：
        // MessageWindowChatMemory.add() 會先呼叫 findByConversationId() 取得現有訊息數，
        // 若此處回傳 sanitized（已壓縮）訊息，數量會小於 DB 實際數，
        // 導致 saveAll() 誤刪 DB 最舊記錄（SYSTEM + USER），破壞對話歷史完整性。
        return repository.findByInterviewIdOrderByCreatedAtAsc(interviewId).stream()
                .map(this::toSpringAiMessage)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        UUID interviewId = UUID.fromString(conversationId);
        List<ConversationMessage> existing = repository.findByInterviewIdOrderByCreatedAtAsc(interviewId);

        if (messages.size() > existing.size()) {
            // 只插入新增的訊息，保留既有訊息的 id/createdAt
            List<Message> newMessages = messages.subList(existing.size(), messages.size());
            for (Message message : newMessages) {
                repository.save(toConversationMessage(interviewId, message));
            }
        } else if (messages.size() < existing.size()) {
            // MessageWindowChatMemory 觸發視窗截斷：刪除最舊的幾筆
            int toDelete = existing.size() - messages.size();
            List<UUID> idsToDelete = existing.subList(0, toDelete).stream()
                    .map(ConversationMessage::getId)
                    .toList();
            repository.deleteAllById(idsToDelete);
        }
        // 數量相同時不做任何操作
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        UUID interviewId = UUID.fromString(conversationId);
        repository.deleteByInterviewId(interviewId);
    }

    private Message toSpringAiMessage(ConversationMessage msg) {
        return switch (msg.getMessageType()) {
            case SYSTEM -> new SystemMessage(msg.getContent() != null ? msg.getContent() : "");
            case USER -> new UserMessage(msg.getContent() != null ? msg.getContent() : "");
            case ASSISTANT -> deserializeAssistantMessage(msg.getContent(), msg.getModel());
            case TOOL -> deserializeToolResponseMessage(msg.getContent());
        };
    }

    private ConversationMessage toConversationMessage(UUID interviewId, Message message) {
        String model = null;
        String content;

        if (message instanceof AssistantMessage am) {
            Object modelMeta = am.getMetadata().get("model");
            if (modelMeta instanceof String s) model = s;

            if (am.hasToolCalls()) {
                // 設計說明：AssistantMessage 帶 toolCalls 時，content 可能為空（模型只發出工具呼叫）。
                // 將 toolCalls 序列化為 JSON 存入 content，以便下一輪還原完整的 AssistantMessage。
                // 官方 JdbcChatMemoryRepository 此處只存 getText()，會丟失 toolCalls 資訊。
                content = serializeAssistantWithToolCalls(am);
            } else {
                content = am.getText();
            }
        } else if (message instanceof ToolResponseMessage trm) {
            // 設計說明：ToolResponseMessage 包含一或多個工具回應（tool name + response data）。
            // 官方 JdbcChatMemoryRepository 存空內容，我們完整序列化以保留工具執行結果。
            // 同時從 metadata 取 model（由 CrossModelChatMemory.tagToolResponseMessage 注入）。
            Object modelMeta = trm.getMetadata().get("model");
            if (modelMeta instanceof String s) model = s;
            content = serializeToolResponses(trm);
        } else {
            content = message.getText();
        }

        return ConversationMessage.create(interviewId, message.getMessageType(), content, model);
    }

    private String serializeAssistantWithToolCalls(AssistantMessage am) {
        try {
            List<Map<String, String>> toolCallMaps = am.getToolCalls().stream()
                    .map(tc -> {
                        Map<String, String> m = new HashMap<>();
                        m.put("id", tc.id());
                        m.put("type", tc.type());
                        m.put("name", tc.name());
                        m.put("arguments", tc.arguments());
                        return m;
                    })
                    .toList();
            Map<String, Object> payload = new HashMap<>();
            payload.put("_type", "assistant_tool_calls");
            payload.put("text", am.getText() != null ? am.getText() : "");
            payload.put("toolCalls", toolCallMaps);
            // 設計說明：持久化 Gemini thinking 模型的 thoughtSignatures（base64 編碼 byte[]），
            // 確保跨 request（重啟/scaling）後歷史訊息仍帶 thought_signature，
            // 避免 Gemini API 400 "Function call is missing a thought_signature" 錯誤。
            // 非 Gemini thinking 模型的 AssistantMessage 不含此 metadata，序列化自動跳過。
            // 參考：https://ai.google.dev/gemini-api/docs/thought-signatures
            Object signaturesObj = am.getMetadata().get("thoughtSignatures");
            if (signaturesObj instanceof List<?> sigList && !sigList.isEmpty()) {
                List<String> base64Sigs = sigList.stream()
                        .filter(s -> s instanceof byte[])
                        .map(s -> Base64.getEncoder().encodeToString((byte[]) s))
                        .toList();
                if (!base64Sigs.isEmpty()) {
                    payload.put("thoughtSignatures", base64Sigs);
                }
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize AssistantMessage toolCalls, falling back to text", e);
            return am.getText() != null ? am.getText() : "";
        }
    }

    private String serializeToolResponses(ToolResponseMessage trm) {
        try {
            List<Map<String, String>> responseMaps = trm.getResponses().stream()
                    .map(r -> {
                        Map<String, String> m = new HashMap<>();
                        m.put("id", r.id());
                        m.put("name", r.name());
                        m.put("responseData", r.responseData());
                        return m;
                    })
                    .toList();
            Map<String, Object> payload = new HashMap<>();
            payload.put("_type", "tool_responses");
            payload.put("responses", responseMaps);
            // 設計說明：持久化 model metadata（由 CrossModelChatMemory.tagToolResponseMessage 注入），
            // 讓 DB 記錄此 TOOL response 對應的 model，即使 MessageWindow 截斷 ASSISTANT(toolCalls)，
            // TOOL response 仍帶有完整上下文，便於跨模型切換場景的診斷。
            Object modelMeta = trm.getMetadata().get("model");
            if (modelMeta instanceof String s) {
                payload.put("model", s);
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize ToolResponseMessage", e);
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private Message deserializeAssistantMessage(String content, String model) {
        if (content == null || content.isBlank()) {
            return AssistantMessage.builder()
                    .content("")
                    .properties(Map.of("model", model != null ? model : ""))
                    .build();
        }
        if (!content.startsWith("{")) {
            // 舊格式純文字，直接回傳（附 model metadata 供 CrossModelChatMemory 判斷來源 provider）
            return AssistantMessage.builder()
                    .content(content)
                    .properties(Map.of("model", model != null ? model : ""))
                    .build();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(content, Map.class);
            if ("assistant_tool_calls".equals(payload.get("_type"))) {
                String text = (String) payload.getOrDefault("text", "");
                List<Map<String, String>> tcMaps = (List<Map<String, String>>) payload.getOrDefault("toolCalls", List.of());
                List<AssistantMessage.ToolCall> toolCalls = tcMaps.stream()
                        .map(tc -> new AssistantMessage.ToolCall(tc.get("id"), tc.get("type"), tc.get("name"), tc.get("arguments")))
                        .toList();
                // 還原 thoughtSignatures 到 AssistantMessage metadata，
                // 讓 GoogleGenAiChatModel.messageToGeminiParts() 能重新附加到 functionCall parts。
                // 同時注入 model 供 CrossModelChatMemory 判斷來源 provider。
                Map<String, Object> properties = new HashMap<>();
                if (model != null) {
                    properties.put("model", model);
                }
                Object sigsObj = payload.get("thoughtSignatures");
                if (sigsObj instanceof List<?> sigList && !sigList.isEmpty()) {
                    List<byte[]> signatures = sigList.stream()
                            .filter(s -> s instanceof String)
                            .map(s -> Base64.getDecoder().decode((String) s))
                            .toList();
                    if (!signatures.isEmpty()) {
                        properties.put("thoughtSignatures", signatures);
                    }
                }
                return AssistantMessage.builder()
                        .content(text)
                        .toolCalls(toolCalls)
                        .properties(properties)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize AssistantMessage JSON, treating as plain text", e);
        }
        return AssistantMessage.builder()
                .content(content)
                .properties(Map.of("model", model != null ? model : ""))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Message deserializeToolResponseMessage(String content) {
        if (content == null || content.isBlank()) {
            return ToolResponseMessage.builder().responses(List.of()).build();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(content, Map.class);
            if ("tool_responses".equals(payload.get("_type"))) {
                List<Map<String, String>> rMaps = (List<Map<String, String>>) payload.getOrDefault("responses", List.of());
                List<ToolResponseMessage.ToolResponse> responses = rMaps.stream()
                        .map(r -> new ToolResponseMessage.ToolResponse(r.get("id"), r.get("name"), r.get("responseData")))
                        .toList();
                // 還原 model metadata，供 toConversationMessage() 持久化時取用
                Map<String, Object> metadata = new HashMap<>();
                Object model = payload.get("model");
                if (model instanceof String s) {
                    metadata.put("model", s);
                }
                return ToolResponseMessage.builder()
                        .responses(responses)
                        .metadata(metadata)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize ToolResponseMessage JSON", e);
        }
        return ToolResponseMessage.builder().responses(List.of()).build();
    }
}
