package com.interview.scoring.domain;

/**
 * 設計說明：拆三欄是為了把「事實」與「LLM 解讀」分開，
 * 面試官 UI 可分別樣式化（時間戳灰底等寬字 / behavior 粗體 / interpretation 一般字），
 * 一眼分辨「真的發生了什麼」與「LLM 怎麼解讀」，不會被 LLM 牽鼻子走。
 * Schema 由 Gemini responseSchema 強制要求所有三個欄位非空。
 */
public record Evidence(
        String timestamp,        // 例：[02:14]
        String behavior,         // 事實層 — Timeline 上的具體動作
        String interpretation    // 解讀層 — 口語白話為何代表 driver/passenger 行為
) {}
