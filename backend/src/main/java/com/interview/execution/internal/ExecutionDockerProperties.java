package com.interview.execution.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Docker 執行環境設定，集中管理 execution.docker.* 屬性。
 *
 * 設計說明：
 * 放置於 execution.internal 套件，因為只有 DockerClientConfiguration 與
 * DockerContainerManager 使用，屬模組內部實作細節。
 * 預設值 unix:///var/run/docker.sock 適用本地開發；
 * GCP 部署時透過 EXECUTION_DOCKER_HOST 環境變數覆蓋為 TCP 位址。
 *
 * @ConfigurationPropertiesScan 已在 InterviewPlatformApplication 啟用，無需額外 @Bean 宣告。
 */
@ConfigurationProperties(prefix = "execution.docker")
record ExecutionDockerProperties(String host) {
    public ExecutionDockerProperties {
        if (host == null || host.isBlank()) {
            host = "unix:///var/run/docker.sock";
        }
    }
}
