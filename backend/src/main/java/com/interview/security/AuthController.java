package com.interview.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin 認證相關端點。
 *
 * 設計說明：
 * 僅在 aci.security.enabled=true 時啟用。
 * GET /me：用途為前端確認 JWT 被後端接受（auth health check），
 * 回傳 JWT claims 中的基本資訊。
 * 使用者完整 profile 由前端從 OIDC user info 取得，不依賴此端點。
 *
 * 登出由前端 oidc-client-ts 的 signoutRedirect() 處理，後端無需介入。
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "aci.security.enabled", havingValue = "true")
public class AuthController {

    /**
     * 確認目前的 JWT 被後端接受，並回傳 token 中的基本 claims。
     */
    @GetMapping("/me")
    public Map<String, Object> getMe(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return Map.of(
                    "name", getClaimOrDefault(jwt, "name", "Admin"),
                    "email", getClaimOrDefault(jwt, "email", ""),
                    "sub", getClaimOrDefault(jwt, "sub", "")
            );
        }
        return Map.of("name", "Admin");
    }

    private String getClaimOrDefault(Jwt jwt, String claim, String defaultValue) {
        String value = jwt.getClaimAsString(claim);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
