package com.interview.interview.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 面試清理排程的外部化配置。
 *
 * 設計為 record + @ConfigurationProperties，遵循專案既有的 AciModelsProperties 慣例。
 * @ConfigurationPropertiesScan 已在 InterviewPlatformApplication 啟用，無需額外 @Bean 宣告。
 *
 * 欄位說明：
 * - gracePeriodMinutes：面試計時結束後額外給予的緩衝時間（分鐘）。
 *   設為 60 分鐘而非 0，是因為排程器本身有固定執行週期，
 *   受測者的瀏覽器計時器與伺服器時鐘可能存在數秒誤差，
 *   寬限期確保不會因排程稍早執行而誤判仍在倒數的面試。
 *
 * - cron：Spring @Scheduled cron 表達式，預設只在上班時間（08:50~18:50）執行，
 *   避免非工作時段做無意義的 DB 查詢。
 */
@ConfigurationProperties(prefix = "interview.cleanup")
public record InterviewCleanupProperties(
        int gracePeriodMinutes,
        String cron
) {
    public InterviewCleanupProperties {
        if (gracePeriodMinutes <= 0) {
            gracePeriodMinutes = 60;
        }
        if (cron == null || cron.isBlank()) {
            cron = "0 50 8-18 * * MON-FRI";
        }
    }
}
