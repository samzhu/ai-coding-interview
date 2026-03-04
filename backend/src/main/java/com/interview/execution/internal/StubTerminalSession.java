package com.interview.execution.internal;

import com.interview.execution.TerminalSession;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * lab profile 下的假終端 session（StubContainerManager 使用）。
 *
 * 設計說明：
 * 在 lab profile（無 Docker 環境）下，模擬一個簡單的 bash 行為：
 * - 連線後立即送出提示符 "$ "
 * - echo 使用者輸入，Enter 後顯示假輸出和新提示符
 * 讓前端 UI 可以正常顯示和測試，不需要真正的 Docker 容器。
 */
class StubTerminalSession implements TerminalSession {

    private final String id = UUID.randomUUID().toString();
    private volatile Consumer<byte[]> outputCallback;
    private volatile boolean alive = true;
    private final StringBuilder lineBuffer = new StringBuilder();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void write(byte[] data) {
        if (!alive || outputCallback == null) return;

        String input = new String(data, StandardCharsets.UTF_8);
        // echo 輸入（xterm.js 預設不自動 echo，需由 server 回送）
        outputCallback.accept(data);

        lineBuffer.append(input);
        // 偵測 Enter（\r 或 \n）後給一個假回應
        if (input.contains("\r") || input.contains("\n")) {
            String line = lineBuffer.toString().trim().replaceAll("[\r\n]", "");
            lineBuffer.setLength(0);
            String response = "\r\n[stub] " + (line.isEmpty() ? "" : "command: " + line + "\r\n") + "$ ";
            outputCallback.accept(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void onOutput(Consumer<byte[]> callback) {
        this.outputCallback = callback;
        // 連線後立即送出初始提示符
        callback.accept("stub-bash$ ".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void resize(int cols, int rows) {
        // no-op：stub 不需要實際 resize
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void close() {
        alive = false;
    }
}
