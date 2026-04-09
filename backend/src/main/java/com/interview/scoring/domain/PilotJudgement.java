package com.interview.scoring.domain;

/**
 * Gemini Judge 的完整輸出結構，對應 Pilot Score 設計文件 §3.4 JSON schema。
 *
 * 設計說明：
 * - subDimensions 用個別欄位而非 Map，是為了讓 Spring AI 的 .entity()
 *   structured output binding 能直接用 reflection 對應 JSON 欄位名。
 * - headline 是 30 秒讀完版（給面試官第一眼用），summary 是完整三段口語說明。
 * - 不在 record 內加任何 validation 邏輯：schema 校驗交給 Gemini responseSchema 處理，
 *   反序列化失敗會在 GeminiPilotJudge 中被捕獲。
 */
public record PilotJudgement(
        double pilotScore,
        PilotVerdict verdict,
        String headline,
        String summary,
        SubDimensions subDimensions
) {
    public record SubDimensions(
            SubScore specification,
            SubScore criticalReview,
            SubScore verification,
            SubScore recovery
    ) {}
}
