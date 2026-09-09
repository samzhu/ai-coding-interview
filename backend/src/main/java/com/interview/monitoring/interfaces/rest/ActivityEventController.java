package com.interview.monitoring.interfaces.rest;

import com.interview.monitoring.domain.ActivityEventRecord;
import com.interview.monitoring.infrastructure.ActivityEventJdbcRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 接收前端 L1 行為事件（file.opened/closed、terminal.test.run、code.manual_edit）。
 *
 * 設計說明：用 HTTP POST 而非 WebSocket，符合「後端服務無狀態」原則。
 * 斷線期間事件遺失可接受 — 候選人此時也無法操作編輯器/終端。
 * 與既有 MonitoringController.@PostMapping("/code") 並存（不同 path 不衝突）。
 */
@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/activity")
public class ActivityEventController {

    private final ActivityEventJdbcRepository repository;

    ActivityEventController(ActivityEventJdbcRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void recordActivity(
            @PathVariable UUID interviewId,
            @RequestBody ActivityEventRequest request) {
        repository.save(ActivityEventRecord.of(
                interviewId, request.eventType(), request.payload()));
    }

    public record ActivityEventRequest(String eventType, Map<String, Object> payload) {}
}
