package com.interview.monitoring.infrastructure;

import com.interview.TestcontainersConfiguration;
import com.interview.monitoring.domain.ActivityEventRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@Import({TestcontainersConfiguration.class, JsonbMapConverters.class})
class ActivityEventJdbcRepositoryTest {

    @Autowired ActivityEventJdbcRepository repository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void saveAndFindByInterviewId_returnsEventsInOrder() {
        var interviewId = UUID.randomUUID();
        // 設計說明：activity event 有 FK 到 interviews(id)，需先 seed parent row
        seedInterview(interviewId);

        var e1 = ActivityEventRecord.of(interviewId, "file.opened",
                Map.of("filePath", "a.py"));
        var e2 = ActivityEventRecord.of(interviewId, "ai.edit.proposal",
                Map.of("proposalId", "p1", "filePath", "a.py"));
        repository.save(e1);
        repository.save(e2);

        var found = repository.findByInterviewIdOrderByOccurredAt(interviewId);
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getEventType()).isEqualTo("file.opened");
        assertThat(found.get(0).getPayload()).containsEntry("filePath", "a.py");
        assertThat(found.get(1).getEventType()).isEqualTo("ai.edit.proposal");
        assertThat(found.get(1).getPayload())
                .containsEntry("proposalId", "p1")
                .containsEntry("filePath", "a.py");
    }

    private void seedInterview(UUID id) {
        // 設計說明：用 raw JdbcTemplate 跳過 aggregate 建構，最小化測試 setup。
        // 欄位來自 001-create-interview-table.sql + 後續 ALTER migration 的 NOT NULL 欄位：
        //   005 加了 question_id NOT NULL（default 已 DROP）
        //   015 加了 duration_minutes NOT NULL DEFAULT 60（有 default，可省略）
        // question_id 使用任意 UUID（測試中沒有 questions 表 FK，此 UUID 僅用於滿足 NOT NULL）。
        jdbc.update("""
                INSERT INTO interviews
                  (id, candidate_id, interviewer_id, title, status, version,
                   question_id, scheduled_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'SCHEDULED', 0,
                        '00000000-0000-0000-0000-000000000001', NOW(), NOW(), NOW())
                """,
                id,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test Interview"
        );
    }
}
