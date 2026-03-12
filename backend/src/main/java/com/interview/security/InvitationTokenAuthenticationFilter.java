package com.interview.security;

import com.interview.invitation.InvitationInfo;
import com.interview.invitation.InvitationQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 驗證 Candidate 的 invitation token（Authorization: Bearer 非 JWT 格式）。
 *
 * 設計說明：
 * Candidate 不走 OAuth2 登入，以邀請碼作為 Bearer token 存取 candidate 端點。
 * JWT token（含有 '.' 字元的三段結構）直接跳過，由後續的 BearerTokenAuthenticationFilter 處理。
 * 非 JWT 的 Bearer token 視為 invitation token，查 DB 驗證後設定 CandidateAuthentication。
 *
 * 若 URL 包含 interviewId（如 /api/v1/interviews/{id}/...），額外驗證 token 對應的
 * interviewId 是否吻合，防止 candidate 以有效 token 存取其他人的面試資源。
 */
public class InvitationTokenAuthenticationFilter extends OncePerRequestFilter {

    // 匹配 /api/v1/interviews/{uuid}/ 路徑中的 UUID
    private static final Pattern INTERVIEW_ID_PATTERN =
            Pattern.compile("/api/v1/interviews/([0-9a-f-]{36})(?:/|$)");

    private final InvitationQueryService invitationQueryService;

    public InvitationTokenAuthenticationFilter(InvitationQueryService invitationQueryService) {
        this.invitationQueryService = invitationQueryService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // JWT 有三段由 '.' 分隔的結構，直接跳過讓 BearerTokenAuthenticationFilter 處理
        if (token.contains(".")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 非 JWT → 視為 invitation token
        Optional<InvitationInfo> invitationOpt = invitationQueryService.findValidInvitation(token);
        if (invitationOpt.isEmpty()) {
            writeUnauthorized(response, "Invalid or expired invitation token");
            return;
        }

        InvitationInfo invitation = invitationOpt.get();

        // 若 URL 含有 interviewId，驗證 token 是否屬於該 interview
        UUID urlInterviewId = extractInterviewId(request.getRequestURI());
        if (urlInterviewId != null && !urlInterviewId.equals(invitation.interviewId())) {
            writeForbidden(response, "Token does not match interview");
            return;
        }

        // 驗證通過，設定 SecurityContext
        CandidateAuthentication auth = new CandidateAuthentication(invitation.id(), invitation.interviewId());
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
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

    private void writeUnauthorized(HttpServletResponse response, String detail) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.getWriter().write(
                "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"" + detail + "\"}");
    }

    private void writeForbidden(HttpServletResponse response, String detail) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json");
        response.getWriter().write(
                "{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"" + detail + "\"}");
    }
}
