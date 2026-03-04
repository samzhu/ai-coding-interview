package com.interview.execution;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * 互動式終端 session 的公開 API。
 *
 * 放在 execution 模組根套件，供 interview 模組的 WebSocket handler 跨模組使用。
 * 設計為 Closeable，讓呼叫端可用 try-with-resources 或明確呼叫 close() 釋放資源。
 */
public interface TerminalSession extends Closeable {

    /** 唯一識別此 session 的 ID（UUID 字串）。 */
    String getId();

    /**
     * 寫入資料到容器 stdin（使用者按鍵輸入）。
     * 非阻塞：寫入管道後立即返回。
     */
    void write(byte[] data);

    /**
     * 註冊 stdout 輸出回調。
     * 每次容器有輸出時，回調會被呼叫並傳入原始位元組。
     * 必須在 session 建立後儘快呼叫，以接收初始輸出（如 bash 提示符）。
     */
    void onOutput(Consumer<byte[]> callback);

    /**
     * 調整終端大小（xterm.js FitAddon 會在 resize 時觸發）。
     * cols/rows 單位為字元數，對應 docker exec resize API。
     */
    void resize(int cols, int rows);

    /** 檢查 session 是否仍然活著（exec process 仍在執行中）。 */
    boolean isAlive();

    /** 關閉 session，釋放 stdin 管道和 exec process。 */
    @Override
    void close();
}
