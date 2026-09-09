package com.interview.monitoring.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityEventRecordTest {

    @Test
    void of_createsRecordWithGeneratedTimestamp() {
        var interviewId = UUID.randomUUID();
        var record = ActivityEventRecord.of(
                interviewId,
                "ai.edit.proposal",
                Map.of("proposalId", "p-001", "filePath", "solution.py")
        );

        assertThat(record.getInterviewId()).isEqualTo(interviewId);
        assertThat(record.getEventType()).isEqualTo("ai.edit.proposal");
        assertThat(record.getPayload()).containsEntry("proposalId", "p-001");
        assertThat(record.getOccurredAt()).isNotNull();
        assertThat(record.getId()).isNull();
    }
}
