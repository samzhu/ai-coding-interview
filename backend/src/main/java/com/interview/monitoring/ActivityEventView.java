package com.interview.monitoring;

import java.time.Instant;
import java.util.Map;

/**
 * Public read-only projection of an activity event for cross-module consumers (scoring).
 *
 * 設計說明：ActivityEventRecord entity 在 com.interview.monitoring.domain 子套件，
 * Spring Modulith 預設視為內部型別。本 record 放在 monitoring 模組根套件作為公開 DTO，
 * 給 scoring TimelineBuilder 重建面試 timeline 用。
 *
 * 不暴露 id、interviewId 欄位 — caller 已經以 interviewId 發 query，
 * 不需要在每個 view 裡重複；id 是內部識別碼，跨模組無意義。
 */
public record ActivityEventView(
        Instant occurredAt,
        String eventType,
        Map<String, Object> payload
) {}
