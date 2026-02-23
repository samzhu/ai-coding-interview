package com.interview.ai.interfaces.rest;

import com.interview.ai.domain.ConversationMessage;

import java.util.List;

public record ConversationHistoryResponse(List<ChatResponse> messages) {

    public static ConversationHistoryResponse from(List<ConversationMessage> messages) {
        return new ConversationHistoryResponse(
                messages.stream().map(ChatResponse::from).toList());
    }
}
