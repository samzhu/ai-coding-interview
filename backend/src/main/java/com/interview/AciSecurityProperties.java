package com.interview;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 安全性相關設定，集中管理 aci.security.* 屬性。
 *
 * 設計說明：
 * 從 @Value 遷移至 @ConfigurationProperties，取得型別安全與 IDE 自動補全。
 * corsAllowedOrigins 型別為 List<String>，Spring Boot relaxed binding 自動將
 * 逗號分隔字串（環境變數 CORS_ALLOWED_ORIGINS）拆分為 List，免去手動 split 邏輯。
 *
 * @ConfigurationPropertiesScan 已在 InterviewPlatformApplication 啟用，無需額外 @Bean 宣告。
 */
@ConfigurationProperties(prefix = "aci.security")
public record AciSecurityProperties(
        boolean enabled,
        List<String> corsAllowedOrigins
) {
    public AciSecurityProperties {
        if (corsAllowedOrigins == null) {
            corsAllowedOrigins = List.of("http://localhost:3000", "http://localhost:3001");
        }
    }
}
