package com.interview.monitoring.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Spring Data JDBC aggregate for interview_activity_events.
 *
 * 設計說明：放在 domain/ 因為這是 monitoring 模組的聚合根。
 * package-private 建構子搭配 of() 工廠方法，避免外部誤建構未持久化的 record。
 * payload 用 Map<String, Object> 對應 JSONB column —— Map↔PGobject 的 Spring Data
 * 轉換器將在 Task 8 (Repository) 同時註冊。
 */
@Table("interview_activity_events")
public class ActivityEventRecord {

    @Id
    private UUID id;
    private UUID interviewId;
    private Instant occurredAt;
    private String eventType;
    private Map<String, Object> payload;

    ActivityEventRecord() {} // for Spring Data instantiation

    private ActivityEventRecord(UUID interviewId, String eventType, Map<String, Object> payload) {
        this.interviewId = interviewId;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = Instant.now();
    }

    public static ActivityEventRecord of(UUID interviewId, String eventType, Map<String, Object> payload) {
        return new ActivityEventRecord(interviewId, eventType, payload);
    }

    public UUID getId() { return id; }
    public UUID getInterviewId() { return interviewId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getEventType() { return eventType; }
    public Map<String, Object> getPayload() { return payload; }
}
