package com.interview.interview.infrastructure.persistence;

import com.interview.TestcontainersConfiguration;
import com.interview.interview.domain.Interview;
import com.interview.interview.domain.InterviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@Import(TestcontainersConfiguration.class)
@DisplayName("InterviewRepository 整合測試")
class InterviewRepositoryTest {

    private static final UUID QUESTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private InterviewRepository repository;

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
}
