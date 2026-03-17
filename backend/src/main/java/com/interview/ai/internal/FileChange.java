package com.interview.ai.internal;

import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 設計說明：editProposal 工具的單檔修改建議。
 * 使用 record + @ToolParam 讓 Spring AI 自動產生 JSON Schema，
 * 模型直接輸出結構化物件，不需雙重 JSON 編碼。
 * 參考：https://docs.spring.io/spring-ai/reference/2.0/api/tools.html
 */
public record FileChange(
        @ToolParam(description = "Relative file path from workspace root, e.g. 'src/main/java/Game.java'")
        String file,
        @ToolParam(description = "Exact current code to replace — must match file content exactly (use readFile to verify)")
        String original,
        @ToolParam(description = "New code to replace the original with")
        String proposed
) {}
