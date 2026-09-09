package com.interview.monitoring.infrastructure;

import com.interview.monitoring.domain.ActivityEventRecord;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

/**
 * 設計說明：用 @Query 顯式寫 SQL 而非依賴 method name derivation，
 * 因為 method name 在 multi-word column 上有時會推導錯誤。
 * <p>
 * Timeline 重建的主要查詢模式是依 interview_id + occurred_at 排序，
 * 對應 idx_activity_events_interview_time 索引。
 */
public interface ActivityEventJdbcRepository extends CrudRepository<ActivityEventRecord, UUID> {

    @Query("SELECT * FROM interview_activity_events " +
           "WHERE interview_id = :interviewId " +
           "ORDER BY occurred_at ASC")
    List<ActivityEventRecord> findByInterviewIdOrderByOccurredAt(UUID interviewId);
}
