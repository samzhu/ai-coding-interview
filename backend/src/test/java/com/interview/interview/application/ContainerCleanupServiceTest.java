package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.interview.domain.Interview;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContainerCleanupService 排程清理邏輯")
class ContainerCleanupServiceTest {

    private static final String QUESTION_ID = "question1";

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private ContainerService containerService;

    @Mock
    private InterviewService interviewService;

    // 使用真實的 Properties 物件（record），不需要 mock
    private InterviewCleanupProperties properties;

    private ContainerCleanupService service;

    @BeforeEach
    void setUp() {
        // 預設寬限期 60 分鐘，符合 application.yml 預設值
        properties = new InterviewCleanupProperties(60, "0 50 8-18 * * MON-FRI");
        service = new ContainerCleanupService(interviewRepository, containerService, interviewService, properties);
    }

    // ── cleanupOrphanedContainers ─────────────────────────────────────────────

    @Test
    @DisplayName("孤兒容器：COMPLETED 面試有 containerId 時應呼叫 stopContainer 並清除")
    void shouldCleanupOrphanedContainerForCompletedInterview() {
        Interview completed = completedInterviewWithContainer("cid-completed");
        when(interviewRepository.findCompletedOrCancelledWithContainer()).thenReturn(List.of(completed));
        when(interviewRepository.findExpiredInProgressInterviews(anyInt(), any())).thenReturn(List.of());

        service.scheduledCleanup();

        verify(containerService).stopContainer("cid-completed");
        verify(interviewRepository).save(completed);
        assertThat(completed.getContainerId()).isNull();
    }

    @Test
    @DisplayName("孤兒容器：無孤兒時不應呼叫 stopContainer")
    void shouldSkipCleanupWhenNoOrphans() {
        when(interviewRepository.findCompletedOrCancelledWithContainer()).thenReturn(List.of());
        when(interviewRepository.findExpiredInProgressInterviews(anyInt(), any())).thenReturn(List.of());

        service.scheduledCleanup();

        verify(containerService, never()).stopContainer(any());
        verify(interviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("孤兒容器：停止失敗時應記錄警告並繼續清理下一個（容錯）")
    void shouldContinueCleanupWhenStopFails() {
        Interview first = completedInterviewWithContainer("cid-fail");
        Interview second = completedInterviewWithContainer("cid-ok");
        when(interviewRepository.findCompletedOrCancelledWithContainer()).thenReturn(List.of(first, second));
        when(interviewRepository.findExpiredInProgressInterviews(anyInt(), any())).thenReturn(List.of());

        // 第一個容器停止失敗
        doThrow(new RuntimeException("Docker error")).when(containerService).stopContainer("cid-fail");

        service.scheduledCleanup();

        // 第一個失敗，第二個仍應成功處理
        verify(containerService).stopContainer("cid-fail");
        verify(containerService).stopContainer("cid-ok");
        // 第二個應正常 save
        verify(interviewRepository).save(second);
    }

    // ── autoCompleteExpiredInterviews ─────────────────────────────────────────

    @Test
    @DisplayName("逾時面試：超過 deadline + gracePeriod 的 IN_PROGRESS 面試應呼叫 completeInterview")
    void shouldAutoCompleteExpiredInterview() {
        Interview expired = inProgressInterview();
        when(interviewRepository.findCompletedOrCancelledWithContainer()).thenReturn(List.of());
        when(interviewRepository.findExpiredInProgressInterviews(anyInt(), any())).thenReturn(List.of(expired));
        when(interviewService.completeInterview(expired.getId())).thenReturn(expired);

        service.scheduledCleanup();

        verify(interviewService).completeInterview(expired.getId());
    }

    @Test
    @DisplayName("逾時面試：無逾時面試時不應呼叫 completeInterview")
    void shouldSkipWhenNoExpiredInterviews() {
        when(interviewRepository.findCompletedOrCancelledWithContainer()).thenReturn(List.of());
        when(interviewRepository.findExpiredInProgressInterviews(anyInt(), any())).thenReturn(List.of());

        service.scheduledCleanup();

        verify(interviewService, never()).completeInterview(any());
    }

    @Test
    @DisplayName("逾時面試：一場 completeInterview 失敗時應繼續處理下一場（容錯）")
    void shouldContinueAutoCompleteWhenOneFails() {
        Interview failInterview = inProgressInterview();
        Interview okInterview = inProgressInterview();
        when(interviewRepository.findCompletedOrCancelledWithContainer()).thenReturn(List.of());
        when(interviewRepository.findExpiredInProgressInterviews(anyInt(), any()))
                .thenReturn(List.of(failInterview, okInterview));

        // 第一場失敗（例如前端已率先完成，樂觀鎖衝突）
        when(interviewService.completeInterview(failInterview.getId()))
                .thenThrow(new IllegalStateException("Already completed"));
        when(interviewService.completeInterview(okInterview.getId())).thenReturn(okInterview);

        service.scheduledCleanup();

        // 兩場都應嘗試
        verify(interviewService).completeInterview(failInterview.getId());
        verify(interviewService).completeInterview(okInterview.getId());
    }

    @Test
    @DisplayName("寬限期應從 properties 正確傳入 findExpiredInProgressInterviews")
    void shouldPassGracePeriodFromProperties() {
        // 使用自訂寬限期 30 分鐘
        var customProperties = new InterviewCleanupProperties(30, "0 50 8-18 * * MON-FRI");
        var customService = new ContainerCleanupService(
                interviewRepository, containerService, interviewService, customProperties);

        when(interviewRepository.findCompletedOrCancelledWithContainer()).thenReturn(List.of());
        when(interviewRepository.findExpiredInProgressInterviews(anyInt(), any())).thenReturn(List.of());

        customService.scheduledCleanup();

        // 驗證傳入的 gracePeriodMinutes 為 30
        ArgumentCaptor<Integer> graceCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(interviewRepository).findExpiredInProgressInterviews(graceCaptor.capture(), any());
        assertThat(graceCaptor.getValue()).isEqualTo(30);
    }

    // ── Helper 工廠方法 ────────────────────────────────────────────────────────

    /**
     * 建立帶有 containerId 的 COMPLETED 面試，模擬孤兒容器場景。
     */
    private Interview completedInterviewWithContainer(String containerId) {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Test Interview", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        interview.start();
        interview.assignContainer(containerId);
        interview.complete();
        return interview;
    }

    /**
     * 建立已開始的 IN_PROGRESS 面試，模擬超時場景（不設定實際 startedAt 偏移；
     * 超時判斷由 Repository 查詢負責，此處只驗證 Service 的呼叫邏輯）。
     */
    private Interview inProgressInterview() {
        Interview interview = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "In Progress Interview", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        interview.start();
        return interview;
    }
}
