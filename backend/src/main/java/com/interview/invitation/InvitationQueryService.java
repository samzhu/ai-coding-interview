package com.interview.invitation;

import java.util.Optional;

/**
 * invitation 模組的跨模組公開查詢介面。
 *
 * 設計說明：
 * 此介面供 security 模組的 InvitationTokenAuthenticationFilter 使用，
 * 以驗證 candidate 的 invitation token。放在模組根套件確保 Spring Modulith
 * 認可其為公開 API，不違反模組邊界。
 *
 * 僅提供 security 驗證所需的最小查詢能力，不暴露 domain 操作（如 join、create）。
 */
public interface InvitationQueryService {

    /**
     * 依 token 查詢有效（未過期）的 invitation。
     * 若 token 不存在或已過期，回傳 empty。
     */
    Optional<InvitationInfo> findValidInvitation(String token);
}
