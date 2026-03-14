package com.interview.ai;

import java.util.UUID;

/**
 * AI 工具執行事件，由 InterviewWorkspaceTools 發布，供 InterviewMonitoringService 廣播給管理員。
 *
 * 設計說明：與 AiChatMessageEvent 同模組根套件，讓 monitoring 模組可以跨模組監聽。
 * state 值：
 * - "running"  — 工具開始執行（顯示 spinner）
 * - "completed" — 工具執行完成（顯示 checkmark）
 * - "error"    — 工具執行失敗（顯示紅色叉）
 */
public record AiToolExecutionEvent(
        UUID interviewId,
        String toolCallId,
        String toolName,
        String state,
        String summary) {
}
