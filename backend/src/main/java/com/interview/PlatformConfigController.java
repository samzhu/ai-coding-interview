package com.interview;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 前端取得平台配置的公開端點。
 *
 * 設計說明：
 * 前端（admin、candidate）在啟動時呼叫此端點，
 * 取得 securityEnabled 旗標來決定是否啟用 OAuth2 登入流程與 invitation token 機制。
 * 此端點永遠公開（SecurityConfig 與 DevSecurityConfig 均設定 permitAll）。
 * securityEnabled 由 AciSecurityProperties 提供，統一來源避免多處 @Value 散落。
 *
 * 位置說明：
 * 移至根套件 com.interview，避免 Spring Modulith 偵測到 security 套件與根套件的循環依賴：
 * root(SecurityConfig→InvitationTokenAuthenticationFilter) ↔ security(PlatformConfigController→AciSecurityProperties)
 * 根套件的類別視為「模組根」，其對各模組的依賴不構成 Modulith 邊界違規。
 */
@RestController
public class PlatformConfigController {

    private final boolean securityEnabled;

    public PlatformConfigController(AciSecurityProperties properties) {
        this.securityEnabled = properties.enabled();
    }

    @GetMapping("/api/v1/config")
    public Map<String, Object> getConfig() {
        return Map.of("securityEnabled", securityEnabled);
    }
}
