package com.interview.ai.infrastructure.persistence;

import com.interview.ai.domain.ConversationMessage;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 實作 Spring AI 的 ChatMemoryRepository 介面，底層使用既有 ai_conversations 表。
 *
 * 為什麼不直接用 JdbcChatMemoryRepository？
 * - 我們需要額外欄位（model）和 FK 關聯（interview_id）
 * - JdbcChatMemoryRepository 使用自己的 SPRING_AI_CHAT_MEMORY 表，schema 不符需求
 * - 透過 adapter pattern，我們保留既有 DB schema，同時符合 Spring AI 介面合約
 *
 * saveAll 採用「增量寫入」策略：
 * - 若新訊息比現有多：只插入新增的部分（保留現有訊息的 id/createdAt）
 * - 若新訊息比現有少：刪除最舊的訊息（MessageWindowChatMemory 的視窗截斷行為）
 * - 若數量相同：不做任何操作
 * 這個策略保留了現有訊息的 UUID 和 createdAt，避免前端 key 不穩定。
 */
@Component
class ConversationChatMemoryRepository implements ChatMemoryRepository {

    private final ConversationMessageRepository repository;

    ConversationChatMemoryRepository(ConversationMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<String> findConversationIds() {
        // 回傳所有有對話記錄的 interview_id，供管理工具或測試使用
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
            case SYSTEM -> new SystemMessage(msg.getContent());
            case USER -> new UserMessage(msg.getContent());
            case ASSISTANT -> new AssistantMessage(msg.getContent());
            // TOOL 訊息不在本系統使用，若出現則視為 USER 訊息（防禦性處理）
            default -> new UserMessage(msg.getContent());
        };
    }

    private ConversationMessage toConversationMessage(UUID interviewId, Message message) {
        // AssistantMessage 可能帶有 model 屬性（由 AiChatService 在串流完成後寫入）
        String model = null;
        if (message instanceof AssistantMessage am) {
            Object modelMeta = am.getMetadata().get("model");
            if (modelMeta instanceof String s) {
                model = s;
            }
        }
        return ConversationMessage.create(interviewId, message.getMessageType(), message.getText(), model);
    }
}
