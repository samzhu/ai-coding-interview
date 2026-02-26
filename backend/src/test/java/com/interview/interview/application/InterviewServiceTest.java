package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.execution.ExamConfig;
import com.interview.execution.ExamConfigService;
import com.interview.interview.InterviewCompletedEvent;
import com.interview.interview.InterviewStartedEvent;
import com.interview.interview.domain.CheckpointResult;
import com.interview.interview.domain.Interview;
import com.interview.interview.domain.InterviewStatus;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import com.interview.interview.interfaces.rest.TimeRemainingResponse;
import com.interview.question.QuestionDetail;
import com.interview.question.QuestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewService 業務邏輯")
class InterviewServiceTest {

    private static final String QUESTION_ID = "question1";

    @Mock
    private InterviewRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CheckpointResultRepository checkpointResultRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private ContainerService containerService;

    @Mock
    private ExamConfigService examConfigService;

    @InjectMocks
    private InterviewService service;

    /** 建立含 2 個 checkpoint 的 ExamConfig stub */
    private static ExamConfig stubExamConfig() {
        var cp1 = new ExamConfig.ExamCheckpoint("1", "CP1 title", "CP1 desc", "./run.sh test1");
        var cp2 = new ExamConfig.ExamCheckpoint("2", "CP2 title", "CP2 desc", "./run.sh test2");
        return new ExamConfig("/workspace", List.of(), List.of(cp1, cp2));
    }

    @Test
    @DisplayName("成功建立面試並儲存")
    void shouldCreateInterviewSuccessfully() {
        var command = new CreateInterviewCommand(
                UUID.randomUUID(), UUID.randomUUID(),
                "Java Backend Interview", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        when(repository.save(any(Interview.class))).thenAnswer(inv -> inv.getArgument(0));

        Interview result = service.createInterview(command);

        assertThat(result.getTitle()).isEqualTo("Java Backend Interview");
        assertThat(result.getInterviewStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(result.getQuestionId()).isEqualTo(QUESTION_ID);
        verify(repository).save(any(Interview.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("成功開始面試，狀態變為 IN_PROGRESS 並從 exam.yml 初始化 checkpoints")
    void shouldStartInterviewSuccessfully() {
        Interview scheduled = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        when(repository.findById(scheduled.getId())).thenReturn(Optional.of(scheduled));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var question = new QuestionDetail(QUESTION_ID, "Hangman", "desc", "MEDIUM", "java",
                null, null, "some-image:latest");
        when(questionService.getQuestion(QUESTION_ID)).thenReturn(question);
        when(containerService.startContainer("some-image:latest")).thenReturn("container-abc");
        when(examConfigService.getExamConfig("container-abc")).thenReturn(stubExamConfig());
        when(checkpointResultRepository.saveAll(any())).thenReturn(List.of());

        Interview result = service.startInterview(scheduled.getId());

        assertThat(result.getInterviewStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(result.getStartedAt()).isNotNull();
        verify(checkpointResultRepository).saveAll(any());
        verify(eventPublisher).publishEvent(any(InterviewStartedEvent.class));
        // exam config 快取應被讀取
        verify(examConfigService).getExamConfig("container-abc");
    }

    @Test
    @DisplayName("開始不存在的面試應拋出例外")
    void shouldThrowWhenStartingNonExistentInterview() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startInterview(unknownId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    @DisplayName("成功完成面試並發布評分事件，停止容器後清除快取")
    void shouldCompleteInterviewAndPublishEvaluationEvent() {
        Interview inProgress = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        inProgress.start();
        inProgress.assignContainer("container-xyz");
        when(repository.findById(inProgress.getId())).thenReturn(Optional.of(inProgress));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Interview result = service.completeInterview(inProgress.getId());

        assertThat(result.getInterviewStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(InterviewCompletedEvent.class));
        // 容器停止後應清除快取
        verify(examConfigService).evict("container-xyz");
    }

    @Test
    @DisplayName("完成已是 SCHEDULED 狀態的面試應拋出例外")
    void shouldThrowWhenCompletingScheduledInterview() {
        Interview scheduled = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        when(repository.findById(scheduled.getId())).thenReturn(Optional.of(scheduled));

        assertThatThrownBy(() -> service.completeInterview(scheduled.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("getTimeRemaining 面試未開始時回傳全部時間")
    void shouldReturnFullTimeWhenNotStarted() {
        Interview scheduled = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        when(repository.findById(scheduled.getId())).thenReturn(Optional.of(scheduled));

        TimeRemainingResponse response = service.getTimeRemaining(scheduled.getId());

        assertThat(response.totalSeconds()).isEqualTo(3600L);
        assertThat(response.remainingSeconds()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("getTimeRemaining 面試進行中時回傳剩餘時間")
    void shouldReturnRemainingTimeWhenInProgress() {
        Interview inProgress = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        inProgress.start();
        when(repository.findById(inProgress.getId())).thenReturn(Optional.of(inProgress));

        TimeRemainingResponse response = service.getTimeRemaining(inProgress.getId());

        assertThat(response.totalSeconds()).isEqualTo(3600L);
        assertThat(response.remainingSeconds()).isGreaterThan(3500L);
        assertThat(response.remainingSeconds()).isLessThanOrEqualTo(3600L);
    }

    @Test
    @DisplayName("ensureContainerRunning 容器仍在運作時直接回傳現有 containerId")
    void shouldReturnExistingContainerWhenRunning() {
        Interview inProgress = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        inProgress.start();
        inProgress.assignContainer("running-container");
        when(repository.findById(inProgress.getId())).thenReturn(Optional.of(inProgress));
        when(containerService.isRunning("running-container")).thenReturn(true);

        String result = service.ensureContainerRunning(inProgress.getId());

        assertThat(result).isEqualTo("running-container");
        verify(containerService, never()).startContainer(any());
    }

    @Test
    @DisplayName("ensureContainerRunning 容器已停止時清除舊快取並啟動新容器")
    void shouldEvictOldCacheAndStartNewContainerWhenStopped() {
        Interview inProgress = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(), "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        inProgress.start();
        inProgress.assignContainer("old-container");
        when(repository.findById(inProgress.getId())).thenReturn(Optional.of(inProgress));
        when(containerService.isRunning("old-container")).thenReturn(false);

        var question = new QuestionDetail(QUESTION_ID, "Q", "desc", "MEDIUM", "java",
                null, null, "some-image:latest");
        when(questionService.getQuestion(QUESTION_ID)).thenReturn(question);
        when(containerService.startContainer("some-image:latest")).thenReturn("new-container");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = service.ensureContainerRunning(inProgress.getId());

        assertThat(result).isEqualTo("new-container");
        // 舊容器快取應被清除
        verify(examConfigService).evict("old-container");
    }
}
