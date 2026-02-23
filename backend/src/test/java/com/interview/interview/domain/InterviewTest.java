package com.interview.interview.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Interview 聚合根行為")
class InterviewTest {

    private final UUID candidateId = UUID.randomUUID();
    private final UUID interviewerId = UUID.randomUUID();
    private final Instant scheduledAt = Instant.now().plusSeconds(3600);
    private final UUID questionId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("建立面試時狀態為 SCHEDULED")
    void scheduleCreatesInterviewWithScheduledStatus() {
        Interview interview = Interview.schedule(candidateId, interviewerId, "Java Interview", scheduledAt, questionId, null);

        assertThat(interview.getId()).isNotNull();
        assertThat(interview.getInterviewStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(interview.getTitle()).isEqualTo("Java Interview");
        assertThat(interview.getCandidateId()).isEqualTo(candidateId);
        assertThat(interview.getInterviewerId()).isEqualTo(interviewerId);
        assertThat(interview.getQuestionId()).isEqualTo(questionId);
        assertThat(interview.getCreatedAt()).isNotNull();
        assertThat(interview.getStartedAt()).isNull();
        assertThat(interview.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("title 為 null 時建立面試應拋出例外")
    void scheduleWithNullTitleThrowsException() {
        assertThatThrownBy(() -> Interview.schedule(candidateId, interviewerId, null, scheduledAt, questionId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    @DisplayName("title 為空白時建立面試應拋出例外")
    void scheduleWithBlankTitleThrowsException() {
        assertThatThrownBy(() -> Interview.schedule(candidateId, interviewerId, "  ", scheduledAt, questionId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    @DisplayName("開始面試後狀態變為 IN_PROGRESS 且記錄 startedAt")
    void startChangesStatusToInProgressAndRecordsStartedAt() {
        Interview interview = Interview.schedule(candidateId, interviewerId, "Java Interview", scheduledAt, questionId, null);

        interview.start();

        assertThat(interview.getInterviewStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(interview.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("對已在進行中的面試呼叫 start() 應拋出例外")
    void startAlreadyInProgressInterviewThrowsException() {
        Interview interview = Interview.schedule(candidateId, interviewerId, "Java Interview", scheduledAt, questionId, null);
        interview.start();

        assertThatThrownBy(interview::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IN_PROGRESS");
    }

    @Test
    @DisplayName("完成面試後狀態變為 COMPLETED 且記錄 completedAt")
    void completeChangesStatusToCompletedAndRecordsCompletedAt() {
        Interview interview = Interview.schedule(candidateId, interviewerId, "Java Interview", scheduledAt, questionId, null);
        interview.start();

        interview.complete();

        assertThat(interview.getInterviewStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(interview.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("對 SCHEDULED 狀態的面試呼叫 complete() 應拋出例外")
    void completeScheduledInterviewThrowsException() {
        Interview interview = Interview.schedule(candidateId, interviewerId, "Java Interview", scheduledAt, questionId, null);

        assertThatThrownBy(interview::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SCHEDULED");
    }
}
