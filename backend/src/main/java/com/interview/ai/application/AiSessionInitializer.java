package com.interview.ai.application;

import com.interview.ai.domain.ConversationMessage;
import com.interview.ai.domain.ConversationRole;
import com.interview.ai.infrastructure.persistence.ConversationMessageRepository;
import com.interview.interview.InterviewStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class AiSessionInitializer {

    private static final String SYSTEM_PROMPT =
            "You are an AI interview assistant helping a software engineering candidate. " +
            "You CAN: clarify problem requirements, suggest high-level approaches, guide debugging by asking questions. " +
            "You CANNOT: write complete solutions, directly give the answer, or provide working code for the challenge. " +
            "Keep responses concise and educational. Encourage the candidate to think through the problem.";

    private final ConversationMessageRepository repository;

    AiSessionInitializer(ConversationMessageRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onInterviewStarted(InterviewStartedEvent event) {
        ConversationMessage systemMsg = ConversationMessage.create(
                event.interviewId(), ConversationRole.SYSTEM, SYSTEM_PROMPT);
        repository.save(systemMsg);
    }
}
