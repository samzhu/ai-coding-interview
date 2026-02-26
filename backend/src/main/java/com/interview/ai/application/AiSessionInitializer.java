package com.interview.ai.application;

import com.interview.interview.InterviewStartedEvent;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class AiSessionInitializer {

    private static final String SYSTEM_PROMPT =
            "You are an AI interview assistant helping a software engineering candidate. " +
            "You CAN: clarify problem requirements, suggest high-level approaches, guide debugging by asking questions. " +
            "You CANNOT: write complete solutions, directly give the answer, or provide working code for the challenge. " +
            "Keep responses concise and educational. Encourage the candidate to think through the problem.";

    private final ChatMemory chatMemory;

    AiSessionInitializer(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @EventListener
    public void onInterviewStarted(InterviewStartedEvent event) {
        // 使用 ChatMemory 介面寫入 SYSTEM prompt，與 Spring AI 官方設計保持一致
        chatMemory.add(event.interviewId().toString(), new SystemMessage(SYSTEM_PROMPT));
    }
}
