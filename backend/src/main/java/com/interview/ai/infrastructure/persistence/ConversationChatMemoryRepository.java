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
        return repository.findByInterviewIdOrderByCreatedAtAsc(interviewId).stream()
                .map(this::toSpringAiMessage)
                .toList();
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
            case ASSISTANT -> deserializeAssistantMessage(msg.getContent());
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
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize ToolResponseMessage", e);
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private Message deserializeAssistantMessage(String content) {
        if (content == null || content.isBlank()) {
            return new AssistantMessage("");
        }
        if (!content.startsWith("{")) {
            // 舊格式純文字，直接回傳
            return new AssistantMessage(content);
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(content, Map.class);
            if ("assistant_tool_calls".equals(payload.get("_type"))) {
                String text = (String) payload.getOrDefault("text", "");
                List<Map<String, String>> tcMaps = (List<Map<String, String>>) payload.getOrDefault("toolCalls", List.of());
                List<AssistantMessage.ToolCall> toolCalls = tcMaps.stream()
                        .map(tc -> new AssistantMessage.ToolCall(
                                tc.get("id"), tc.get("type"), tc.get("name"), tc.get("arguments")))
                        .toList();
                return AssistantMessage.builder()
                        .content(text)
                        .toolCalls(toolCalls)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize AssistantMessage JSON, treating as plain text", e);
        }
        return new AssistantMessage(content);
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
                        .map(r -> new ToolResponseMessage.ToolResponse(
                                r.get("id"), r.get("name"), r.get("responseData")))
                        .toList();
                return ToolResponseMessage.builder().responses(responses).build();
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize ToolResponseMessage JSON", e);
        }
        return ToolResponseMessage.builder().responses(List.of()).build();
    }
}
