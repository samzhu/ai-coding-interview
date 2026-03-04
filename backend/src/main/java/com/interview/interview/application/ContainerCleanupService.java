package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.interview.domain.Interview;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 定期清理殭屍容器與自動完成逾時面試的排程服務。
 *
 * 解決兩個關鍵問題：
 * 1. 孤兒容器：伺服器崩潰或非正常關閉時，COMPLETED/CANCELLED 面試可能殘留 container_id，
 *    正常流程已有 completeInterview/cancelInterview 清理，此處為安全網。
 *
 * 2. 殭屍面試：受測者關閉瀏覽器後，IN_PROGRESS 面試計時器仍在前端倒數，
 *    但若瀏覽器永久關閉，就沒有人呼叫 POST /complete，容器永遠不會被回收。
 *    斷線策略決策：不暫停計時器（與 HackerRank/LeetCode 行為一致），
 *    由伺服器端在超過 deadline + 寬限期後自動完成。
 *
 * 並發安全：
 * Interview.complete() 有 canTransitionTo() 狀態檢查 + @Version 樂觀鎖，
 * 若前端 POST /complete 與此排程同時觸發，後者會拋出 IllegalStateException 被 catch 住，
 * 不影響其他面試的處理。
 */
@Service
public class ContainerCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ContainerCleanupService.class);

    private final InterviewRepository interviewRepository;
    private final ContainerService containerService;
    private final InterviewService interviewService;
    private final InterviewCleanupProperties properties;

    public ContainerCleanupService(InterviewRepository interviewRepository,
                                   ContainerService containerService,
                                   InterviewService interviewService,
                                   InterviewCleanupProperties properties) {
        this.interviewRepository = interviewRepository;
        this.containerService = containerService;
        this.interviewService = interviewService;
        this.properties = properties;
    }

    /**
     * 排程入口，在上班時間每小時 50 分執行。
     *
     * 選擇整點前 10 分（50 分）而非整點，是為了避免與其他系統排程集中在整點衝突。
     * cron 表達式從 InterviewCleanupProperties 讀取，允許環境覆寫（測試或特殊需求）。
     *
     * 依序執行：先清孤兒（快速）、再自動完成逾時面試（含容器回收）。
     */
    @Scheduled(cron = "${interview.cleanup.cron:0 50 8-18 * * MON-FRI}")
    public void scheduledCleanup() {
        log.info("Starting scheduled interview cleanup");
        cleanupOrphanedContainers();
        autoCompleteExpiredInterviews();
        log.info("Scheduled interview cleanup finished");
    }

    /**
     * 清理孤兒容器：COMPLETED/CANCELLED 面試仍殘留 container_id 的情況。
     *
     * 使用精確查詢 findCompletedOrCancelledWithContainer() 取代 findAll() + Java 過濾，
     * 避免在面試資料量大時載入全表。
     * 此方法不加 @Transactional：每場面試單獨 save，
     * 一場失敗（容器不存在或 Docker 錯誤）不應回滾其他場的清理。
     */
    private void cleanupOrphanedContainers() {
        var orphans = interviewRepository.findCompletedOrCancelledWithContainer();
        if (orphans.isEmpty()) {
            return;
        }
        log.info("Found {} orphaned container(s) to clean up", orphans.size());
        for (Interview interview : orphans) {
            String containerId = interview.getContainerId();
            try {
                log.info("Cleaning up orphaned container {} for {} interview {}",
                        containerId, interview.getInterviewStatus(), interview.getId());
                containerService.stopContainer(containerId);
                interview.clearContainer();
                interviewRepository.save(interview);
            } catch (Exception e) {
                log.warn("Failed to clean up orphaned container {} for interview {}: {}",
                        containerId, interview.getId(), e.getMessage());
            }
        }
    }

    /**
     * 自動完成逾時的 IN_PROGRESS 面試並回收容器。
     *
     * 寬限期（gracePeriodMinutes）從 properties 讀取，預設 60 分鐘，
     * 確保排程延遲或前後端時鐘誤差不會誤判仍在倒數的面試。
     *
     * 不加 @Transactional：interviewService.completeInterview() 已有自己的交易邊界，
     * 每場面試在獨立交易內完成（stop container → complete → clearContainer → publish event），
     * 一場失敗不應影響其他場的自動完成。
     */
    private void autoCompleteExpiredInterviews() {
        int gracePeriodMinutes = properties.gracePeriodMinutes();
        var expired = interviewRepository.findExpiredInProgressInterviews(gracePeriodMinutes, Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        log.info("Found {} expired interview(s) to auto-complete (grace period: {} min)",
                expired.size(), gracePeriodMinutes);
        for (Interview interview : expired) {
            try {
                log.info("Auto-completing expired interview {} (started: {}, duration: {} min)",
                        interview.getId(), interview.getStartedAt(), interview.getDurationMinutes());
                interviewService.completeInterview(interview.getId());
            } catch (Exception e) {
                // IllegalStateException 代表前端已率先完成（樂觀鎖競爭），屬正常情況，降級為 debug
                log.debug("Could not auto-complete interview {} (may have been completed concurrently): {}",
                        interview.getId(), e.getMessage());
            }
        }
    }
}
