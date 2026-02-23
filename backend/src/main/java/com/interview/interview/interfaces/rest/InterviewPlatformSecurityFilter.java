package com.interview.interview.interfaces.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 簡易 Admin API 靜態 Bearer Token 認證過濾器。
 * 只保護 /api/v1/admin/** 路徑。
 * Token 從環境變數 ADMIN_TOKEN 讀取。
 * 若 ADMIN_TOKEN 未設定，Admin 路徑不受保護（開發環境）。
 */
@Component
@Order(1)
public class InterviewPlatformSecurityFilter extends OncePerRequestFilter {

    private final String adminToken;

    public InterviewPlatformSecurityFilter(
            @Value("${admin.token:}") String adminToken) {
        this.adminToken = adminToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 只保護 /api/v1/admin/** 路徑
        if (!path.startsWith("/api/v1/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 若未設定 ADMIN_TOKEN，跳過認證（開發環境）
        if (adminToken == null || adminToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String expected = "Bearer " + adminToken;

        if (authHeader == null || !authHeader.equals(expected)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/problem+json");
            response.getWriter().write(
                    "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Valid admin token required\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
