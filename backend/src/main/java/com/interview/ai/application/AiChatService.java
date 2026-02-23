package com.interview.ai.application;

import com.interview.ai.AiChatMessageEvent;
import com.interview.ai.domain.ConversationMessage;
import com.interview.ai.domain.ConversationRole;
import com.interview.ai.infrastructure.persistence.ConversationMessageRepository;
import com.interview.ai.internal.AiModelRegistry;
import com.interview.interview.InterviewAiPolicyProvider;
import com.interview.interview.InterviewExpiredException;
import com.interview.interview.InterviewModelProvider;
import com.interview.interview.InterviewTimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final ConversationMessageRepository repository;
    private final AiModelRegistry modelRegistry;
    private final InterviewModelProvider interviewModelProvider;
    private final InterviewAiPolicyProvider aiPolicyProvider;
    private final InterviewTimeProvider interviewTimeProvider;
    private final ApplicationEventPublisher eventPublisher;

    public AiChatService(ConversationMessageRepository repository,
                         AiModelRegistry modelRegistry,
                         InterviewModelProvider interviewModelProvider,
                         InterviewAiPolicyProvider aiPolicyProvider,
                         InterviewTimeProvider interviewTimeProvider,
                         ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.modelRegistry = modelRegistry;
        this.interviewModelProvider = interviewModelProvider;
        this.aiPolicyProvider = aiPolicyProvider;
        this.interviewTimeProvider = interviewTimeProvider;
        this.eventPublisher = eventPublisher;
    }

    public ConversationMessage chat(UUID interviewId, String userMessage) {
        if (!aiPolicyProvider.isAiEnabled(interviewId)) {
            throw new AiDisabledException("此階段 AI 不可用，請獨立完成題目");
        }
        if (interviewTimeProvider.isExpired(interviewId)) {
            throw new InterviewExpiredException("面試時間已到，無法使用 AI");
        }
        ConversationMessage userMsg = ConversationMessage.create(interviewId, ConversationRole.USER, userMessage);
        repository.save(userMsg);
        eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, "USER", userMessage));

        List<ConversationMessage> history = repository.findByInterviewIdOrderByCreatedAtAsc(interviewId);
        String response = generateResponse(interviewId, history);

        ConversationMessage assistantMsg = ConversationMessage.create(interviewId, ConversationRole.ASSISTANT, response);
        ConversationMessage saved = repository.save(assistantMsg);
        eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, "ASSISTANT", response));
        return saved;
    }

    public record StreamingChatResult(Flux<String> tokenStream, UUID messageId) {}

    public StreamingChatResult streamChat(UUID interviewId, String userMessage) {
        if (!aiPolicyProvider.isAiEnabled(interviewId)) {
            throw new AiDisabledException("此階段 AI 不可用，請獨立完成題目");
        }
        if (interviewTimeProvider.isExpired(interviewId)) {
            throw new InterviewExpiredException("面試時間已到，無法使用 AI");
        }
        ConversationMessage userMsg = ConversationMessage.create(interviewId, ConversationRole.USER, userMessage);
        repository.save(userMsg);
        eventPublisher.publishEvent(new AiChatMessageEvent(interviewId, "USER", userMessage));

        List<ConversationMessage> history = repository.findByInterviewIdOrderByCreatedAtAsc(interviewId);

        UUID assistantMsgId = UUID.randomUUID();

        Optional<ChatClient> chatClient = resolveChatClient(interviewId);
        if (chatClient.isEmpty()) {
            String stub = "AI assistant is not configured for this environment. " +
                    "Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.";
            repository.save(ConversationMessage.create(interviewId, ConversationRole.ASSISTANT, stub));
            return new StreamingChatResult(Flux.just(stub), assistantMsgId);
        }

        String systemPrompt = extractSystemPrompt(history);
        List<Message> conversationMessages = extractConversationMessages(history);

        StringBuilder fullResponse = new StringBuilder();
        var promptSpec = chatClient.get().prompt();
        if (!systemPrompt.isEmpty()) {
            promptSpec = promptSpec.system(systemPrompt);
        }
        Flux<String> stream = promptSpec
                .messages(conversationMessages)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    try {
                        repository.save(ConversationMessage.create(
                                interviewId, ConversationRole.ASSISTANT, fullResponse.toString()));
                        eventPublisher.publishEvent(new AiChatMessageEvent(
                                interviewId, "ASSISTANT", fullResponse.toString()));
                    } catch (Exception e) {
                        log.error("Failed to save assistant message after streaming", e);
                    }
                })
                .doOnError(e -> log.error("AI stream failed for interview {}", interviewId, e));

        return new StreamingChatResult(stream, assistantMsgId);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> getHistory(UUID interviewId) {
        return repository.findByInterviewIdOrderByCreatedAtAsc(interviewId);
    }

    private String generateResponse(UUID interviewId, List<ConversationMessage> history) {
        Optional<ChatClient> chatClient = resolveChatClient(interviewId);
        if (chatClient.isEmpty()) {
            log.warn("No ChatClient available for interview {}. Returning stub response.", interviewId);
            return "AI assistant is not configured for this environment. " +
                    "Set GOOGLE_GENAI_API_KEY to enable AI-powered hints.";
        }

        try {
            String systemPrompt = extractSystemPrompt(history);
            List<Message> conversationMessages = extractConversationMessages(history);

            var promptSpec = chatClient.get().prompt();
            if (!systemPrompt.isEmpty()) {
                promptSpec = promptSpec.system(systemPrompt);
            }
            return promptSpec
                    .messages(conversationMessages)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI chat call failed", e);
            return "AI assistant temporarily unavailable. Please try again later.";
        }
    }

    private Optional<ChatClient> resolveChatClient(UUID interviewId) {
        String modelId = interviewModelProvider.getAiModel(interviewId);
        Optional<ChatClient> client = modelRegistry.getChatClient(modelId);
        if (client.isEmpty()) {
            log.warn("Model '{}' not found in registry, trying first available", modelId);
            return modelRegistry.getFirstClient();
        }
        return client;
    }

    private String extractSystemPrompt(List<ConversationMessage> history) {
        return history.stream()
                .filter(m -> m.getConversationRole() == ConversationRole.SYSTEM)
                .map(ConversationMessage::getContent)
                .collect(Collectors.joining("\n"));
    }

    private List<Message> extractConversationMessages(List<ConversationMessage> history) {
        return history.stream()
                .filter(m -> m.getConversationRole() != ConversationRole.SYSTEM)
                .map(this::toSpringAiMessage)
                .toList();
    }

    private Message toSpringAiMessage(ConversationMessage msg) {
        return switch (msg.getConversationRole()) {
            case SYSTEM -> new SystemMessage(msg.getContent());
            case USER -> new UserMessage(msg.getContent());
            case ASSISTANT -> new AssistantMessage(msg.getContent());
        };
    }
}
