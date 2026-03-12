package com.interview.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

/**
 * 代表已通過 invitation token 驗證的 Candidate 認證物件。
 *
 * 設計說明：
 * Candidate 不走 OAuth2 登入，透過邀請碼取得存取權。
 * ROLE_CANDIDATE 授權僅允許操作與面試相關的 candidate 端點，
 * admin 端點需 ROLE_ADMIN（OAuth2 JWT）。
 */
public class CandidateAuthentication extends AbstractAuthenticationToken {

    private final UUID invitationId;
    private final UUID interviewId;

    public CandidateAuthentication(UUID invitationId, UUID interviewId) {
        super(List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")));
        this.invitationId = invitationId;
        this.interviewId = interviewId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return invitationId.toString();
    }

    public UUID getInvitationId() {
        return invitationId;
    }

    public UUID getInterviewId() {
        return interviewId;
    }
}
