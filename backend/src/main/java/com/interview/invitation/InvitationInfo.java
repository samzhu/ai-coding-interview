package com.interview.invitation;

import java.util.UUID;

/**
 * 跨模組查詢 Invitation 的公開 DTO。
 *
 * 設計說明：
 * 放在 invitation 模組根套件（com.interview.invitation），
 * 確保 Spring Modulith 允許其他模組（如 security 模組）合法存取。
 * 只暴露 security filter 所需的最小欄位，不包含 domain 內部細節。
 */
public record InvitationInfo(UUID id, UUID interviewId, String token) {}
