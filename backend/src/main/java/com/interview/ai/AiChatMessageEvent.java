package com.interview.ai;

import org.springframework.ai.chat.messages.MessageType;

import java.util.UUID;

/**
 * AI 對話訊息事件，由 AiChatService 發布後供 InterviewMonitoringService 廣播給管理員。
 * role 改用 Spring AI 的 MessageType（USER/ASSISTANT），語義更明確，不需依賴字串比對。
 */
public record AiChatMessageEvent(UUID interviewId, MessageType role, String content) {
}
