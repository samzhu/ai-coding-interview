package com.interview.interview.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterviewStatus 狀態轉換規則")
class InterviewStatusTest {

    @Test
    @DisplayName("SCHEDULED 可以轉換到 IN_PROGRESS")
    void scheduledCanTransitionToInProgress() {
        assertThat(InterviewStatus.SCHEDULED.canTransitionTo(InterviewStatus.IN_PROGRESS)).isTrue();
    }

    @Test
    @DisplayName("SCHEDULED 可以轉換到 CANCELLED")
    void scheduledCanTransitionToCancelled() {
        assertThat(InterviewStatus.SCHEDULED.canTransitionTo(InterviewStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("SCHEDULED 不能直接轉換到 COMPLETED")
    void scheduledCannotTransitionToCompleted() {
        assertThat(InterviewStatus.SCHEDULED.canTransitionTo(InterviewStatus.COMPLETED)).isFalse();
    }

    @Test
    @DisplayName("IN_PROGRESS 可以轉換到 COMPLETED")
    void inProgressCanTransitionToCompleted() {
        assertThat(InterviewStatus.IN_PROGRESS.canTransitionTo(InterviewStatus.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("IN_PROGRESS 可以轉換到 CANCELLED")
    void inProgressCanTransitionToCancelled() {
        assertThat(InterviewStatus.IN_PROGRESS.canTransitionTo(InterviewStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("IN_PROGRESS 不能轉換回 SCHEDULED")
    void inProgressCannotTransitionToScheduled() {
        assertThat(InterviewStatus.IN_PROGRESS.canTransitionTo(InterviewStatus.SCHEDULED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(InterviewStatus.class)
    @DisplayName("COMPLETED 不能轉換到任何狀態")
    void completedCannotTransitionToAnything(InterviewStatus target) {
        assertThat(InterviewStatus.COMPLETED.canTransitionTo(target)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(InterviewStatus.class)
    @DisplayName("CANCELLED 不能轉換到任何狀態")
    void cancelledCannotTransitionToAnything(InterviewStatus target) {
        assertThat(InterviewStatus.CANCELLED.canTransitionTo(target)).isFalse();
    }
}
