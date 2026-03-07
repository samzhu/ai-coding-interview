package com.interview.ai.interfaces.rest;

import com.interview.ai.domain.ConversationMessage;
import org.springframework.ai.chat.messages.MessageType;

import java.util.List;

public record ConversationHistoryResponse(List<ChatResponse> messages) {

    public static ConversationHistoryResponse from(List<ConversationMessage> messages) {
        return new ConversationHistoryResponse(
                messages.stream()
                        // 設計說明：只回傳 USER 和有純文字內容的 ASSISTANT 訊息。
                        // TOOL 和帶 toolCalls 的 ASSISTANT 是 AI 內部 tool calling 的中間產物，
                        // 前端已透過 SSE data-tool-invocation 即時顯示工具狀態，不需要再次呈現。
                        // JSON 格式的 ASSISTANT（帶 toolCalls）以 "{" 開頭，過濾掉避免前端解析錯誤。
                        .filter(m -> m.getMessageType() == MessageType.USER
                                || (m.getMessageType() == MessageType.ASSISTANT
                                    && m.getContent() != null
                                    && !m.getContent().startsWith("{")))
                        .map(ChatResponse::from)
                        .toList());
    }
}
