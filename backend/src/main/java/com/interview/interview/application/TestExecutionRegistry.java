package com.interview.interview.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry for tracking active test execution processes.
 *
 * 設計說明：使用 ConcurrentHashMap 管理短命的執行 process（最多 300 秒）。
 * 不需持久化：server 重啟後 polling 回 404，前端 catch 後 setRunning(false)。
 * @Scheduled 每 60 秒清理超過 5 分鐘的過期 entry，防止記憶體洩漏。
 */
@Component
public class TestExecutionRegistry {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionRegistry.class);
    private static final Duration EXPIRY = Duration.ofMinutes(5);

    private final ConcurrentHashMap<UUID, TestExecutionProcess> processes = new ConcurrentHashMap<>();

    public TestExecutionProcess create(UUID processId) {
        var process = new TestExecutionProcess(processId);
        processes.put(processId, process);
        return process;
    }

    public Optional<TestExecutionProcess> get(UUID processId) {
        return Optional.ofNullable(processes.get(processId));
    }

    /** Clean up entries older than 5 minutes to prevent memory leaks. */
    @Scheduled(fixedRate = 60_000)
    void cleanup() {
        Instant cutoff = Instant.now().minus(EXPIRY);
        int removed = 0;
        for (var entry : processes.entrySet()) {
            if (entry.getValue().getStartedAt().isBefore(cutoff)) {
                processes.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("TestExecutionRegistry cleaned up {} expired entries", removed);
        }
    }
}
