package com.interview.ai.interfaces.rest;

import com.interview.ai.domain.ConversationMessage;
import org.springframework.ai.chat.messages.MessageType;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ConversationHistoryResponse(List<ChatResponse> messages) {

    /**
     * 設計說明：將對話訊息轉換為 Admin 可讀格式。
     *
     * - USER 訊息：直接回傳
     * - ASSISTANT 純文字：直接回傳
     * - ASSISTANT with toolCalls（JSON 以 "{" 開頭，_type=assistant_tool_calls）：
     *   解析每個 toolCall，生成 role=TOOL_CALL 的摘要條目，讓 Admin 看到 AI 讀了哪些檔案、跑了什麼指令
     * - TOOL 回應訊息：略過（工具呼叫已由 TOOL_CALL 條目代表）
     */
    @SuppressWarnings("unchecked")
    public static ConversationHistoryResponse from(List<ConversationMessage> messages, ObjectMapper objectMapper) {
        List<ChatResponse> result = new ArrayList<>();

        for (ConversationMessage m : messages) {
            if (m.getMessageType() == MessageType.USER) {
                result.add(ChatResponse.from(m));
                continue;
            }
            if (m.getMessageType() == MessageType.TOOL) {
                // TOOL 回應為工具執行結果，已由前面的 TOOL_CALL 條目代表，略過
                continue;
            }
            if (m.getMessageType() == MessageType.ASSISTANT) {
                String content = m.getContent();
                if (content == null || !content.startsWith("{")) {
                    // 純文字 ASSISTANT 回應
                    result.add(ChatResponse.from(m));
                    continue;
                }
                // 解析 assistant_tool_calls JSON，生成人類可讀的 TOOL_CALL 條目
                try {
                    Map<String, Object> payload = objectMapper.readValue(content, Map.class);
                    if ("assistant_tool_calls".equals(payload.get("_type"))) {
                        List<Map<String, String>> toolCalls =
                                (List<Map<String, String>>) payload.getOrDefault("toolCalls", List.of());
                        for (Map<String, String> tc : toolCalls) {
                            String summary = buildToolSummary(tc, objectMapper);
                            result.add(ChatResponse.toolCall(m.getId(), summary, m.getCreatedAt()));
                        }
                    }
                    // 若不是 assistant_tool_calls 格式（或解析失敗），略過（避免前端顯示 raw JSON）
                } catch (Exception ignored) {
                    // 解析失敗：略過此條訊息
                }
            }
        }

        return new ConversationHistoryResponse(result);
    }

    /**
     * 將工具呼叫轉換為中文摘要，例如「讀取檔案 Game.java」、「執行指令 ./gradlew test」。
     */
    @SuppressWarnings("unchecked")
    private static String buildToolSummary(Map<String, String> tc, ObjectMapper objectMapper) {
        String name = tc.getOrDefault("name", "unknown");
        String args = tc.getOrDefault("arguments", "{}");
        return switch (name) {
            case "listDirectory" -> {
                String path = extractStringArg(args, "path", objectMapper);
                yield "列出目錄 " + (path != null && !path.isBlank() ? path : "/");
            }
            case "directoryTree" -> "顯示目錄樹";
            case "readFile" -> {
                String path = extractStringArg(args, "path", objectMapper);
                yield "讀取檔案 " + (path != null ? path : "");
            }
            case "runCommand" -> {
                String command = extractStringArg(args, "command", objectMapper);
                String display = command != null
                        ? (command.length() > 60 ? command.substring(0, 57) + "..." : command)
                        : "";
                yield "執行指令 " + display;
            }
            case "editProposal" -> "提交修改建議";
            default -> name;
        };
    }

    @SuppressWarnings("unchecked")
    private static String extractStringArg(String argsJson, String key, ObjectMapper objectMapper) {
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            Object val = args.get(key);
            return val instanceof String s ? s : null;
        } catch (Exception e) {
            return null;
        }
    }
}
