package com.interview;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全域 CORS 設定。
 *
 * 設計說明：
 * Admin 採用 SPA + Bearer token 模式，不再需要 credentials（Cookie）。
 * allowCredentials(false) 搭配 allowedOriginPatterns 即可支援跨域 API 呼叫。
 * 允許的 origins 透過 aci.security.cors-allowed-origins 設定，
 * 預設含 localhost:3000（admin）與 localhost:3001（candidate）以利本地開發。
 * prod 環境透過 CORS_ALLOWED_ORIGINS 環境變數設為實際域名。
 * corsAllowedOrigins 由 AciSecurityProperties 提供（List<String>），
 * Spring Boot relaxed binding 自動將逗號分隔字串拆分，免去手動 split 邏輯。
 */
@Configuration
class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    CorsConfig(AciSecurityProperties properties) {
        this.allowedOrigins = properties.corsAllowedOrigins().toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("x-vercel-ai-ui-message-stream", "x-accel-buffering")
                .allowCredentials(false);
    }
}
