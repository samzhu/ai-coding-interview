package com.interview.interview.infrastructure.persistence;

import com.interview.TestcontainersConfiguration;
import com.interview.interview.domain.Interview;
import com.interview.interview.domain.InterviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@Import(TestcontainersConfiguration.class)
@DisplayName("InterviewRepository 整合測試")
class InterviewRepositoryTest {

    private static final String QUESTION_ID = "question1";

    @Autowired
    private InterviewRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("儲存面試後應可以透過 ID 查詢")
    void shouldSaveAndFindInterviewById() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Java Integration Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);

        Interview saved = repository.save(interview);
        Optional<Interview> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Java Integration Test");
        assertThat(found.get().getInterviewStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getQuestionId()).isEqualTo(QUESTION_ID);
    }

    @Test
    @DisplayName("開始面試後狀態應持久化為 IN_PROGRESS")
    void shouldPersistInProgressStatus() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Status Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        Interview saved = repository.save(interview);

        saved.start();
        Interview updated = repository.save(saved);

        assertThat(updated.getInterviewStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(updated.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("完成面試後狀態應持久化為 COMPLETED")
    void shouldPersistCompletedStatus() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Complete Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        Interview saved = repository.save(interview);
        saved.start();
        Interview inProgress = repository.save(saved);

        inProgress.complete();
        Interview completed = repository.save(inProgress);

        assertThat(completed.getInterviewStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("刪除面試後應查不到")
    void shouldDeleteInterview() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Delete Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        Interview saved = repository.save(interview);

        repository.deleteById(saved.getId());

        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    // ── findCompletedOrCancelledWithContainer ─────────────────────────────────

    @Test
    @DisplayName("findCompletedOrCancelledWithContainer：COMPLETED 且有 containerId 的面試應被查到")
    void shouldFindCompletedInterviewWithContainer() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Orphan Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        interview.start();
        interview.assignContainer("orphan-cid-1");
        interview.complete();
        repository.save(interview);

        List<Interview> result = repository.findCompletedOrCancelledWithContainer();

        assertThat(result).anyMatch(i -> "orphan-cid-1".equals(i.getContainerId()));
    }

    @Test
    @DisplayName("findCompletedOrCancelledWithContainer：IN_PROGRESS 面試不應被查到")
    void shouldNotFindInProgressInterview() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "In Progress", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        interview.start();
        interview.assignContainer("active-cid");
        repository.save(interview);

        List<Interview> result = repository.findCompletedOrCancelledWithContainer();

        assertThat(result).noneMatch(i -> "active-cid".equals(i.getContainerId()));
    }

    @Test
    @DisplayName("findCompletedOrCancelledWithContainer：COMPLETED 但 containerId 為 null 不應被查到")
    void shouldNotFindCompletedInterviewWithoutContainer() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Clean Completed", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        interview.start();
        interview.complete();
        // 不呼叫 assignContainer，模擬正常流程已清除容器
        repository.save(interview);

        List<Interview> result = repository.findCompletedOrCancelledWithContainer();

        // 不應因為這筆記錄而有結果（containerId 為 null）
        assertThat(result).noneMatch(i -> i.getContainerId() == null
                && InterviewStatus.COMPLETED == i.getInterviewStatus());
    }

    // ── findExpiredInProgressInterviews ───────────────────────────────────────

    @Test
    @DisplayName("findExpiredInProgressInterviews：startedAt 已超過 deadline + 寬限期的面試應被查到")
    void shouldFindExpiredInProgressInterview() {
        // 建立 durationMinutes = 60 的面試，然後將 started_at 回撥 121 分鐘前，
        // 配合 gracePeriodMinutes = 60，確保已超過 60 + 60 = 120 分鐘的容忍上限。
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Expired Interview", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        interview.start();
        Interview saved = repository.save(interview);

        // 直接用 JdbcTemplate 回撥 started_at，繞過 domain 物件的時間設定限制，
        // 模擬面試已於 121 分鐘前啟動的資料庫狀態。
        // PostgreSQL JDBC driver 不認識 java.time.Instant，需要轉為 OffsetDateTime。
        OffsetDateTime pastStart = Instant.now().minusSeconds(121 * 60).atOffset(ZoneOffset.UTC);
        jdbcTemplate.update("UPDATE interviews SET started_at = ? WHERE id = ?",
                pastStart, saved.getId());

        List<Interview> result = repository.findExpiredInProgressInterviews(60, Instant.now());

        assertThat(result).anyMatch(i -> i.getId().equals(saved.getId()));
    }

    @Test
    @DisplayName("findExpiredInProgressInterviews：尚未超過 deadline 的面試不應被查到")
    void shouldNotFindNonExpiredInterview() {
        // 建立剛啟動的面試（startedAt ≈ 現在），不應被查到
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Active Interview", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        interview.start();
        repository.save(interview);

        // gracePeriodMinutes = 60，duration = 60，共 120 分鐘後才算逾時
        List<Interview> result = repository.findExpiredInProgressInterviews(60, Instant.now());

        assertThat(result).noneMatch(i ->
                InterviewStatus.IN_PROGRESS == i.getInterviewStatus()
                        && i.getStartedAt() != null
                        && i.getStartedAt().isAfter(Instant.now().minusSeconds(60)));
    }

    @Test
    @DisplayName("findExpiredInProgressInterviews：COMPLETED 面試即使 started_at 很久以前也不應被查到")
    void shouldNotFindCompletedInterviewAsExpired() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Completed Long Ago", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        interview.start();
        Interview saved = repository.save(interview);
        saved.complete();
        Interview completed = repository.save(saved);

        // 回撥 started_at 至很久以前
        // PostgreSQL JDBC driver 不認識 java.time.Instant，需要轉為 OffsetDateTime。
        OffsetDateTime longAgo = Instant.now().minusSeconds(200 * 60).atOffset(ZoneOffset.UTC);
        jdbcTemplate.update("UPDATE interviews SET started_at = ? WHERE id = ?",
                longAgo, completed.getId());

        List<Interview> result = repository.findExpiredInProgressInterviews(60, Instant.now());

        assertThat(result).noneMatch(i -> i.getId().equals(completed.getId()));
    }
}
