package com.interview.ai;

import java.time.Instant;

/**
 * 設計說明：跨模組暴露的 conversation view，避免把 internal entity（ConversationMessage）
 * 漏出 ai 模組邊界。scoring 模組只需要這幾個欄位來重建對話 timeline；
 * token 數、model 版本等內部細節不必對外暴露。
 */
public record AiConversationView(
        String role,        // "USER" | "ASSISTANT" | "SYSTEM"
        String content,
        Instant createdAt
) {}
