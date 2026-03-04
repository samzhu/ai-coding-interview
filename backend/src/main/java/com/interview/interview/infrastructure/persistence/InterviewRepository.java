package com.interview.interview.infrastructure.persistence;

import com.interview.interview.domain.Interview;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InterviewRepository extends ListCrudRepository<Interview, UUID> {

    /**
     * 查詢已結束（COMPLETED / CANCELLED）但仍殘留 container_id 的面試。
     *
     * 這類記錄是伺服器崩潰或非正常關閉時遺留的孤兒容器，
     * 正常流程下 completeInterview / cancelInterview 都會呼叫 clearContainer()，
     * 因此只有異常情況才會出現，不需要掃全表。
     */
    @Query("SELECT * FROM interviews WHERE status IN ('COMPLETED', 'CANCELLED') AND container_id IS NOT NULL")
    List<Interview> findCompletedOrCancelledWithContainer();

    /**
     * 查詢「已逾時」的進行中面試。
     *
     * 條件：status = IN_PROGRESS 且 started_at 不為 null（排除尚未啟動的邊際情況），
     * 且 started_at + duration_minutes + gracePeriodMinutes 已早於 :now。
     *
     * 使用 PostgreSQL make_interval(mins => int) 而非 INTERVAL 字串常量，
     * 原因是 duration_minutes 是每場面試各自的欄位值，必須在 SQL 中動態計算；
     * 若改為 Java 過濾則需 findAll() 載入全表，不符合大資料量場景。
     * gracePeriodMinutes 作為寬限期，給予伺服器端排程器額外的緩衝，
     * 避免因排程延遲而誤判仍在倒數的面試。
     */
    @Query("SELECT * FROM interviews WHERE status = 'IN_PROGRESS' AND started_at IS NOT NULL " +
            "AND (started_at + make_interval(mins => duration_minutes + :gracePeriodMinutes)) < :now")
    List<Interview> findExpiredInProgressInterviews(@Param("gracePeriodMinutes") int gracePeriodMinutes,
                                                    @Param("now") Instant now);
}
