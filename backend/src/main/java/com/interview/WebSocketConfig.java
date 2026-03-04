package com.interview;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 應用程式級 WebSocket 配置。
 *
 * 放在根套件（與 CorsConfig 同層），因為 WebSocket 是跨模組的基礎設施。
 * URL 模式 /api/v1/interviews/*\/terminal 使用 Ant 風格萬用字元，
 * 其中 * 匹配單一路徑段（即面試 UUID），與 REST 路徑風格對齊。
 *
 * 設計說明：
 * 注入型別使用 WebSocketHandler 介面而非具體的 TerminalWebSocketHandler 類別，
 * 避免 root 模組直接依賴 interview 模組的 internal type（Spring Modulith 模組邊界要求）。
 * @Qualifier 透過 bean 名稱進行 runtime 注入，不引入靜態型別依賴。
 *
 * allowedOrigins("*") 搭配 CorsConfig 統一管理跨域策略。
 * 注意：WebSocket 不使用 Spring MVC 的 CORS 配置，需要在此獨立設定。
 */
@Configuration
@EnableWebSocket
class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler terminalHandler;

    WebSocketConfig(@Qualifier("terminalWebSocketHandler") WebSocketHandler terminalHandler) {
        this.terminalHandler = terminalHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(terminalHandler, "/api/v1/interviews/*/terminal")
                .setAllowedOrigins("*");
    }
}
