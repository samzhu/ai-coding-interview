package com.interview;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 開發模式安全設定（aci.security.enabled=false，預設）。
 *
 * 設計說明：
 * 開發時期不需要認證，所有 API 公開存取，方便本地測試與 CI。
 * 透過 @ConditionalOnProperty 確保只有在 ACI_SECURITY_ENABLED=false（或未設定）時啟用，
 * 與 SecurityConfig 互斥，不會同時生效。
 *
 * 同時定義此 SecurityFilterChain Bean，覆蓋 Spring Security auto-configuration 的預設行為
 * （避免出現 "Using generated security password" 提示）。
 *
 * @EnableWebSecurity 不需要明確標注：Spring Boot security auto-configuration 已透過
 * SecurityAutoConfiguration 啟用。明確標注反而會在 @WebMvcTest 切片測試中
 * 干擾 auto-configuration 的 HttpSecurity 初始化順序。
 */
@Configuration
@ConditionalOnProperty(name = "aci.security.enabled", havingValue = "false", matchIfMissing = true)
public class DevSecurityConfig {

    @Bean
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
