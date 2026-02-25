package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.interview.InterviewExpiredException;
import com.interview.interview.InterviewTimeProvider;
import com.interview.interview.domain.Interview;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import com.interview.question.QuestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckpointProgressService 超時檢查")
class CheckpointProgressServiceTest {

    private static final String QUESTION_ID = "question1";

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private CheckpointResultRepository checkpointResultRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private ContainerService containerService;

    @Mock
    private InterviewService interviewService;

    @Mock
    private InterviewTimeProvider interviewTimeProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CheckpointProgressService service;

    @Test
    @DisplayName("超時時 submitCode 應拋出 InterviewExpiredException")
    void shouldThrowWhenExpiredOnSubmit() {
        Interview inProgress = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null, 1);
        inProgress.start();
        UUID interviewId = inProgress.getId();
        String checkpointId = "1";

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(inProgress));
        when(interviewTimeProvider.isExpired(interviewId)).thenReturn(true);

        var command = new SubmitCodeCommand(interviewId, checkpointId);

        assertThatThrownBy(() -> service.submitCode(command))
                .isInstanceOf(InterviewExpiredException.class)
                .hasMessageContaining("時間已到");
    }

    @Test
    @DisplayName("超時時 runTests 應拋出 InterviewExpiredException")
    void shouldThrowWhenExpiredOnRunTests() {
        Interview inProgress = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null, 1);
        inProgress.start();
        UUID interviewId = inProgress.getId();
        String checkpointId = "1";

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(inProgress));
        when(interviewTimeProvider.isExpired(interviewId)).thenReturn(true);

        assertThatThrownBy(() -> service.runTests(interviewId, checkpointId))
                .isInstanceOf(InterviewExpiredException.class)
                .hasMessageContaining("時間已到");
    }
}
