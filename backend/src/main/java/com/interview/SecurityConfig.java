package com.interview;

import com.interview.invitation.InvitationQueryService;
import com.interview.security.InvitationTokenAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * 生產模式安全設定（aci.security.enabled=true）。
 *
 * 設計說明（SPA + Resource Server 模式）：
 * - Admin（面試官）：前端透過 oidc-client-ts（PKCE）直接向 OIDC Provider 取得 access token，
 *   以 Authorization: Bearer <jwt> 呼叫後端；後端純 Resource Server，只驗 JWT。
 * - Candidate（受測者）：invitation token 作為 Bearer token，
 *   InvitationTokenFilter 查 DB 驗證 → ROLE_CANDIDATE。
 *
 * Filter 執行順序：
 * 1. InvitationTokenAuthenticationFilter — 非 JWT Bearer token → candidate 驗證
 * 2. BearerTokenAuthenticationFilter     — JWT Bearer token → admin 驗證
 *
 * CORS 設定：
 * cors(Customizer.withDefaults()) 讓 Spring Security filter chain 處理 OPTIONS preflight，
 * 確保跨域請求在到達授權層前先通過 CORS 驗證，避免 preflight 被 401 攔截。
 *
 * 設定要求（環境變數，aci.security.enabled=true 時必填）：
 * - SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://auth-dev.omnihubs.cloud
 */
/**
 * @EnableWebSecurity 不需要明確標注：Spring Boot security auto-configuration 已透過
 * SecurityAutoConfiguration 啟用。
 */
@Configuration
@ConditionalOnProperty(name = "aci.security.enabled", havingValue = "true")
public class SecurityConfig {

    private final InvitationQueryService invitationQueryService;

    public SecurityConfig(InvitationQueryService invitationQueryService) {
        this.invitationQueryService = invitationQueryService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var invitationTokenFilter = new InvitationTokenAuthenticationFilter(invitationQueryService);

        // JWT converter：Admin 的 access token 授予 ROLE_ADMIN
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt ->
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CORS preflight（OPTIONS）需在授權層前放行，withDefaults() 使用 CorsConfig 設定
                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth
                        // 公開端點
                        .requestMatchers("/api/v1/invitations/**").permitAll()
                        .requestMatchers("/api/v1/config").permitAll()
                        .requestMatchers("/api/v1/ai/models").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Admin 專用端點（ROLE_ADMIN）
                        .requestMatchers(HttpMethod.POST, "/api/v1/interviews").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/interviews").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/interviews/*/cancel").hasRole("ADMIN")
                        .requestMatchers("/api/v1/interviews/*/invitation").hasRole("ADMIN")
                        .requestMatchers("/api/v1/interviews/*/monitor/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/questions/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/auth/**").hasRole("ADMIN")

                        // Candidate 端點（ROLE_CANDIDATE 或 ROLE_ADMIN）
                        .requestMatchers(HttpMethod.GET, "/api/v1/interviews/*").hasAnyRole("ADMIN", "CANDIDATE")
                        .requestMatchers("/api/v1/interviews/*/checkpoints/**").hasAnyRole("ADMIN", "CANDIDATE")
                        .requestMatchers("/api/v1/interviews/*/files/**").hasAnyRole("ADMIN", "CANDIDATE")
                        .requestMatchers("/api/v1/interviews/*/ai/**").hasAnyRole("ADMIN", "CANDIDATE")
                        .requestMatchers("/api/v1/interviews/*/activity/**").hasAnyRole("ADMIN", "CANDIDATE")
                        .requestMatchers("/api/v1/interviews/*/time-remaining").hasAnyRole("ADMIN", "CANDIDATE")
                        .requestMatchers("/api/v1/interviews/*/container-status").hasAnyRole("ADMIN", "CANDIDATE")
                        .requestMatchers("/api/v1/executions").hasAnyRole("ADMIN", "CANDIDATE")

                        .anyRequest().authenticated()
                )

                // JWT Bearer token 驗證（Admin 的 access token）
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                )

                // Filter 順序：InvitationToken → BearerToken
                .addFilterBefore(invitationTokenFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
