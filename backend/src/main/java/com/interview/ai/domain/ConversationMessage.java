package com.interview.ai.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

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

    public static ConversationMessage create(UUID interviewId, ConversationRole role, String content) {
        return create(interviewId, role, content, null);
    }

    public static ConversationMessage create(UUID interviewId, ConversationRole role, String content, String model) {
        ConversationMessage msg = new ConversationMessage();
        msg.id = UUID.randomUUID();
        msg.interviewId = interviewId;
        msg.role = role.name();
        msg.content = content;
        msg.createdAt = Instant.now();
        msg.model = model;
        return msg;
    }

    public ConversationRole getConversationRole() {
        return ConversationRole.valueOf(this.role);
    }

    public UUID getId() { return id; }
    public UUID getInterviewId() { return interviewId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
