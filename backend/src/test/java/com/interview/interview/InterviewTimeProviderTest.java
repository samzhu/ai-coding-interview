package com.interview.interview;

import com.interview.interview.domain.Interview;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewTimeProvider 超時查詢")
class InterviewTimeProviderTest {

    private static final UUID QUESTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private InterviewRepository repository;

    @InjectMocks
    private InterviewTimeProvider provider;

    @Test
    @DisplayName("面試未開始時 isExpired 回傳 false")
    void shouldReturnFalseWhenNotStarted() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        when(repository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThat(provider.isExpired(interview.getId())).isFalse();
    }

    @Test
    @DisplayName("面試進行中且未超時時 isExpired 回傳 false")
    void shouldReturnFalseWhenInProgressNotExpired() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null, 60);
        interview.start();
        when(repository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThat(provider.isExpired(interview.getId())).isFalse();
    }

    @Test
    @DisplayName("面試不存在時 isExpired 回傳 false")
    void shouldReturnFalseWhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThat(provider.isExpired(unknownId)).isFalse();
    }

    @Test
    @DisplayName("面試進行中且未開始時 getRemainingSeconds 回傳 empty")
    void shouldReturnEmptyWhenNotStarted() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        when(repository.findById(interview.getId())).thenReturn(Optional.of(interview));

        assertThat(provider.getRemainingSeconds(interview.getId()).isPresent()).isFalse();
    }

    @Test
    @DisplayName("面試進行中時 getRemainingSeconds 回傳剩餘秒數")
    void shouldReturnRemainingSecondsWhenInProgress() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null, 60);
        interview.start();
        when(repository.findById(interview.getId())).thenReturn(Optional.of(interview));

        var remaining = provider.getRemainingSeconds(interview.getId());
        assertThat(remaining.isPresent()).isTrue();
        assertThat(remaining.getAsLong()).isGreaterThan(3500L);
        assertThat(remaining.getAsLong()).isLessThanOrEqualTo(3600L);
    }
}
