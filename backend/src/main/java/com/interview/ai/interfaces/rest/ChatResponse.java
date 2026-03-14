package com.interview.ai.interfaces.rest;

import com.interview.ai.domain.ConversationMessage;

import java.time.Instant;
import java.util.UUID;

public record ChatResponse(
        UUID id,
        String role,
        String content,
        Instant createdAt,
        String model,
        Integer promptTokens,
        Integer completionTokens) {

    public static ChatResponse from(ConversationMessage message) {
        return new ChatResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt(),
                message.getModel(),
                message.getPromptTokens(),
                message.getCompletionTokens());
    }

    /**
     * 建立工具呼叫摘要條目，供 Admin 歷史紀錄顯示 AI 使用了哪些工具。
     * role 為 "TOOL_CALL"（非 DB 儲存值，為衍生的展示用 role）。
     */
    public static ChatResponse toolCall(UUID id, String summary, Instant createdAt) {
        return new ChatResponse(id, "TOOL_CALL", summary, createdAt, null, null, null);
    }
}
