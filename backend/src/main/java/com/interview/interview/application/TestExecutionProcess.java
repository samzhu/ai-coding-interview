package com.interview.interview.application;

import java.time.Instant;
import java.util.UUID;

/**
 * In-memory representation of a running test execution process.
 *
 * 設計說明：Thread-safe — appendOutput 由 @Async 執行緒呼叫，getter 由 HTTP polling 執行緒呼叫。
 * StringBuffer 保證 append 的 atomicity；volatile 確保 status/exitCode/passed 的 visibility。
 * Process 是短命的（最多 300 秒 Docker exec），不需持久化到 DB。
 */
public class TestExecutionProcess {

    private final UUID processId;
    private final Instant startedAt;
    private volatile String status; // RUNNING, COMPLETED, FAILED
    private final StringBuffer output; // thread-safe append
    private volatile Integer exitCode; // null while RUNNING
    private volatile Boolean passed;   // null while RUNNING

    TestExecutionProcess(UUID processId) {
        this.processId = processId;
        this.startedAt = Instant.now();
        this.status = "RUNNING";
        this.output = new StringBuffer();
        this.exitCode = null;
        this.passed = null;
    }

    public UUID getProcessId() {
        return processId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public String getStatus() {
        return status;
    }

    public String getConsoleOutput() {
        return output.toString();
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void appendOutput(String text) {
        output.append(text);
    }

    public void complete(int exitCode, boolean passed) {
        this.exitCode = exitCode;
        this.passed = passed;
        this.status = "COMPLETED";
    }

    public void fail(String errorMessage) {
        this.output.append("\n[ERROR] ").append(errorMessage);
        this.exitCode = -1;
        this.passed = false;
        this.status = "FAILED";
    }
}
