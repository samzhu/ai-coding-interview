package com.interview;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 共用 @WebMvcTest 測試配置。
 *
 * 設計說明：
 * @WebMvcTest 預設不載入以下項目，需在切片測試中明確補充：
 *
 * 1. @ConfigurationProperties beans：
 *    CorsConfig（實作 WebMvcConfigurer）依賴 AciSecurityProperties，
 *    @EnableConfigurationProperties(AciSecurityProperties.class) 確保它可被注入。
 *
 * 2. Spring Security HttpSecurity 基礎設施：
 *    @WebMvcTest 自動載入 OAuth2ResourceServerAutoConfiguration，但未載入
 *    提供 HttpSecurity prototype bean 的 ServletWebSecurityAutoConfiguration。
 *    @ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class) 補充此缺口。
 *
 * 3. 測試用寬鬆 SecurityFilterChain（最高優先級）：
 *    @Order(1) 確保 testPermitAllFilterChain 最先被 Spring Security 匹配，
 *    允許所有請求通過，讓控制器切片測試聚焦在業務邏輯而非安全驗證。
 *    OAuth2ResourceServerAutoConfiguration 的 jwtSecurityFilterChain 仍存在，
 *    但因為 @Order 較低而不會被優先匹配。
 *
 * 各 @WebMvcTest 測試透過 @Import(WebMvcTestSliceConfig.class) 引入此配置。
 */
@TestConfiguration
@EnableConfigurationProperties(AciSecurityProperties.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
public class WebMvcTestSliceConfig {

    @Bean
    @Order(1)
    SecurityFilterChain testPermitAllFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
