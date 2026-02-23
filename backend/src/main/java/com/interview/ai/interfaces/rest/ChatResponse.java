package com.interview.ai.interfaces.rest;

import com.interview.ai.domain.ConversationMessage;

import java.time.Instant;
import java.util.UUID;

public record ChatResponse(
        UUID id,
        String role,
        String content,
        Instant createdAt) {

    public static ChatResponse from(ConversationMessage message) {
        return new ChatResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt());
    }
}
