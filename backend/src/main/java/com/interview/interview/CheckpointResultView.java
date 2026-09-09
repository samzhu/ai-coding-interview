package com.interview.interview;

import java.time.Instant;

/**
 * Public read-only projection of CheckpointResult for cross-module consumers (scoring).
 *
 * 設計說明：CheckpointResult entity 在 com.interview.interview.domain 子套件，
 * Spring Modulith 預設視為內部型別，禁止跨模組引用 (MODULITH_TYPE_REF_VIOLATION)。
 * 本 record 放在 interview 模組根套件作為公開 DTO，只暴露 scoring 計算與
 * timeline 重建所需欄位 — submittedCode 等內部狀態不外漏。
 *
 * 欄位選擇對應 Pilot Score 需求：
 * - status: OutcomeCalculator 計算 passed / total 的依據
 * - passedAt / createdAt / updatedAt: TimelineBuilder 排序與展示時間戳
 * - submissionCount: Recovery 維度的「重試次數」訊號
 * - executionOutput: TimelineBuilder 在 timeline 上顯示失敗原因供 LLM judge 引用
 */
public record CheckpointResultView(
        String checkpointId,
        int checkpointSequence,
        String status,
        Instant passedAt,
        Instant createdAt,
        Instant updatedAt,
        int submissionCount,
        String executionOutput
) {}
