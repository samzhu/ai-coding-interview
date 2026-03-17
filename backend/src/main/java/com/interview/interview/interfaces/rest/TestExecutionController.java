package com.interview.interview.interfaces.rest;

import com.interview.interview.application.TestExecutionRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Execution status polling endpoint.
 *
 * 設計說明：前端 submit 後收到 processId，每 2 秒 polling 此端點取得即時 console output。
 * Process 存在 in-memory registry（TestExecutionRegistry），不需持久化。
 * 回傳 consoleOutput 為累積全量（前端直接替換顯示，無需 diff 合併）。
 */
@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/executions")
class TestExecutionController {

    private final TestExecutionRegistry registry;

    TestExecutionController(TestExecutionRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/{processId}")
    ExecutionStatusResponse getStatus(@PathVariable UUID interviewId,
                                      @PathVariable UUID processId) {
        var process = registry.get(processId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + processId));
        return ExecutionStatusResponse.from(process);
    }
}
