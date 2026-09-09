package com.interview;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AciSecurityProperties")
class AciSecurityPropertiesTest {

    @Test
    @DisplayName("預設 CORS origin patterns 支援 localhost 與 LAN/IP 開發入口")
    void defaultCorsAllowedOriginsShouldSupportLocalAndLanDevelopment() {
        var properties = new AciSecurityProperties(false, null);

        assertThat(properties.corsAllowedOrigins())
                .containsExactly(
                        "http://localhost:3000",
                        "http://localhost:3001",
                        "http://127.0.0.1:3000",
                        "http://127.0.0.1:3001",
                        "http://*:3000",
                        "http://*:3001"
                );
    }

    @Test
    @DisplayName("Spring CORS pattern 接受 172 LAN admin origin")
    void defaultCorsAllowedOriginsShouldMatchLanAdminOrigin() {
        var properties = new AciSecurityProperties(false, null);
        var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(properties.corsAllowedOrigins());

        assertThat(configuration.checkOrigin("http://172.25.136.90:3000"))
                .isEqualTo("http://172.25.136.90:3000");
    }
}
