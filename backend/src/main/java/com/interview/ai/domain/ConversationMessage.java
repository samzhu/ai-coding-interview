package com.interview.ai.domain;

import org.springframework.ai.chat.messages.MessageType;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 對話訊息 DB 實體（ai_conversations 表）。
 *
 * role 欄位改用 Spring AI 的 MessageType（USER/ASSISTANT/SYSTEM）取代自訂的 ConversationRole，
 * 好處是不需要在應用層做額外的 enum 轉換，DB 儲存值不變（MessageType.name() 與原 ConversationRole.name() 相同）。
 */
@Table("ai_conversations")
public class ConversationMessage {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column("interview_id")
    private UUID interviewId;

    private String role;

    private String content;

    @Column("created_at")
    private Instant createdAt;

    @Column("model")
    private String model;

    protected ConversationMessage() {
    }

    public static ConversationMessage create(UUID interviewId, MessageType role, String content) {
        return create(interviewId, role, content, null);
    }

    public static ConversationMessage create(UUID interviewId, MessageType role, String content, String model) {
        ConversationMessage msg = new ConversationMessage();
        msg.id = UUID.randomUUID();
        msg.interviewId = interviewId;
        msg.role = role.name();
        msg.content = content;
        msg.createdAt = Instant.now();
        msg.model = model;
        return msg;
    }

    /**
     * 回傳 Spring AI 的 MessageType，方便與 Spring AI 訊息型別互動時無需手動轉換。
     */
    public MessageType getMessageType() {
        return MessageType.valueOf(this.role);
    }

    public UUID getId() { return id; }
    public UUID getInterviewId() { return interviewId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public String getModel() { return model; }
}
