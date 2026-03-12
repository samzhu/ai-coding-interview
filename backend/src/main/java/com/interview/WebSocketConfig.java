package com.interview;

import com.interview.invitation.InvitationInfo;
import com.interview.invitation.InvitationQueryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 應用程式級 WebSocket 配置。
 *
 * 設計說明：
 * 放在根套件，因為 WebSocket 是跨模組的基礎設施。
 * URL 模式 /api/v1/interviews/*\/terminal 使用 Ant 風格萬用字元。
 *
 * 當 aci.security.enabled=true 時，加入 TokenHandshakeInterceptor 驗證：
 * - 從 query parameter ?token= 取得 invitation token
 * - 透過 InvitationQueryService 查 DB 驗證 token 有效性
 * - 驗證 token 對應的 interviewId 是否與 URL 中的 {id} 一致
 * - 驗證通過才允許 WebSocket 握手
 *
 * 注入型別使用 WebSocketHandler 介面，避免直接依賴 interview 模組的 internal type。
 */
@Configuration
@EnableWebSocket
class WebSocketConfig implements WebSocketConfigurer {

    private static final Pattern INTERVIEW_ID_PATTERN =
            Pattern.compile("/api/v1/interviews/([0-9a-f-]{36})/terminal");

    private final WebSocketHandler terminalHandler;
    private final boolean securityEnabled;
    private final InvitationQueryService invitationQueryService;

    WebSocketConfig(@Qualifier("terminalWebSocketHandler") WebSocketHandler terminalHandler,
                    AciSecurityProperties properties,
                    InvitationQueryService invitationQueryService) {
        this.terminalHandler = terminalHandler;
        this.securityEnabled = properties.enabled();
        this.invitationQueryService = invitationQueryService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        var registration = registry
                .addHandler(terminalHandler, "/api/v1/interviews/*/terminal")
                .setAllowedOrigins("*");

        if (securityEnabled) {
            registration.addInterceptors(new TokenHandshakeInterceptor());
        }
    }

    /**
     * WebSocket 握手前驗證 invitation token。
     * Candidate 連接終端機時透過 ?token= 傳遞 invitation token。
     */
    private class TokenHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes)
                throws Exception {

            if (!(request instanceof ServletServerHttpRequest servletRequest)) {
                return false;
            }

            String token = servletRequest.getServletRequest().getParameter("token");
            if (token == null || token.isBlank()) {
                return false;
            }

            Optional<InvitationInfo> invitationOpt = invitationQueryService.findValidInvitation(token);
            if (invitationOpt.isEmpty()) {
                return false;
            }

            InvitationInfo invitation = invitationOpt.get();

            // 驗證 URL 中的 interviewId 與 token 對應的 interviewId 一致
            String path = request.getURI().getPath();
            UUID urlInterviewId = extractInterviewId(path);
            if (urlInterviewId == null || !urlInterviewId.equals(invitation.interviewId())) {
                return false;
            }

            // 儲存 candidate 資訊供 WebSocket handler 使用
            attributes.put("invitationId", invitation.id());
            attributes.put("interviewId", invitation.interviewId());
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
            // 無需後置處理
        }

        private UUID extractInterviewId(String path) {
            Matcher m = INTERVIEW_ID_PATTERN.matcher(path);
            if (m.find()) {
                try {
                    return UUID.fromString(m.group(1));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            return null;
        }
    }
}
