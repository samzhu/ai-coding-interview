package com.interview.ai.internal;

/**
 * AI 提出的程式碼修改提案（edit proposal）。
 *
 * 設計說明：
 * AI 在回應中使用結構化的 edit_proposal XML 區塊表達修改建議，
 * 後端解析後透過 SSE data 事件傳至前端，讓候選人以 diff 視圖審查並決定是否套用。
 * 採用「人在迴路中」模式確保候選人對程式碼變更有完整控制權。
 *
 * @param filePath  相對於 workspace 的檔案路徑，例如 "main.py"
 * @param original  原始程式碼片段（必須完全符合當前檔案內容，供 String.replace 使用）
 * @param proposed  建議修改後的程式碼
 */
public record EditProposal(String filePath, String original, String proposed) {}
