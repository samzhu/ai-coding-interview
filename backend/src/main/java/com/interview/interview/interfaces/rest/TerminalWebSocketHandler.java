package com.interview.interview.interfaces.rest;

import com.interview.execution.ExamConfigService;
import com.interview.execution.TerminalSession;
import com.interview.interview.application.InterviewService;
import com.interview.interview.application.TerminalSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler：處理候選人終端機的即時雙向通訊。
 *
 * 通訊協定（輕量前綴，避免 JSON 解析延遲）：
 * ┌─────────────────────────────────────────────────────────┐
 * │ 方向              │ 格式              │ 說明            │
 * ├─────────────────────────────────────────────────────────┤
 * │ Client → Server  │ i<data>           │ 終端輸入（按鍵）│
 * │ Client → Server  │ r<cols>,<rows>    │ 終端 resize     │
 * │ Server → Client  │ o<data>           │ 終端輸出        │
 * │ Server → Client  │ e<message>        │ 錯誤訊息        │
 * └─────────────────────────────────────────────────────────┘
 *
 * 設計說明：
 * - ConcurrentWebSocketSessionDecorator 包裝 session，確保多執行緒同時寫入時的執行緒安全。
 *   （docker 輸出回調和 WebSocket message 處理在不同執行緒）
 * - InterviewService.ensureContainerRunning() 負責驗證面試狀態和容器活性，
 *   若容器尚未 READY，會拋出 ContainerNotReadyException，此時送錯誤訊息並關閉連線。
 * - URL 格式：/api/v1/interviews/{interviewId}/terminal
 *   interviewId 從 URI 路徑倒數第二段解析（terminal 之前）。
 */
@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TerminalWebSocketHandler.class);

    // WebSocket sendTimeLimit (ms) 和 bufferSizeLimit (bytes)
    private static final int WS_SEND_TIME_LIMIT_MS = 5_000;
    private static final int WS_BUFFER_SIZE_LIMIT = 128 * 1024;

    private final TerminalSessionService terminalSessionService;
    private final InterviewService interviewService;
    private final ExamConfigService examConfigService;

    // WebSocket session ID → TerminalSession（連線存活期間持有）
    private final Map<String, TerminalSession> terminalByWs = new ConcurrentHashMap<>();
    // WebSocket session ID → interviewId（close 時找回 interview 以清理）
    private final Map<String, UUID> interviewByWs = new ConcurrentHashMap<>();

    public TerminalWebSocketHandler(TerminalSessionService terminalSessionService,
                                    InterviewService interviewService,
                                    ExamConfigService examConfigService) {
        this.terminalSessionService = terminalSessionService;
        this.interviewService = interviewService;
        this.examConfigService = examConfigService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession rawSession) {
        // URL: /api/v1/interviews/{interviewId}/terminal
        String path = rawSession.getUri().getPath();
        String[] segments = path.split("/");
        UUID interviewId;
        try {
            // "terminal" 是最後一段，interviewId 在倒數第二段
            interviewId = UUID.fromString(segments[segments.length - 2]);
        } catch (Exception e) {
            sendErrorAndClose(rawSession, "Invalid interview ID in URL: " + path);
            return;
        }

        // 使用 ConcurrentWebSocketSessionDecorator 確保跨執行緒輸出的執行緒安全
        ConcurrentWebSocketSessionDecorator safeSession =
                new ConcurrentWebSocketSessionDecorator(rawSession, WS_SEND_TIME_LIMIT_MS, WS_BUFFER_SIZE_LIMIT);

        try {
            // 確認容器正在執行（同時驗證面試狀態）
            String containerId = interviewService.ensureContainerRunning(interviewId);
            // 從容器讀取 exam.yml，取得 workspace 路徑作為 bash 工作目錄
            String workDir = examConfigService.getExamConfig(containerId).effectiveWorkspace();

            TerminalSession terminal = terminalSessionService.createSession(interviewId, containerId, workDir);

            // 輸出回調：容器 stdout → WebSocket "o" 前綴訊息
            terminal.onOutput(bytes -> {
                if (!safeSession.isOpen()) return;
                try {
                    String text = new String(bytes, StandardCharsets.UTF_8);
                    safeSession.sendMessage(new TextMessage("o" + text));
                } catch (Exception e) {
                    log.debug("WS send failed for session {}: {}", rawSession.getId(), e.getMessage());
                }
            });

            terminalByWs.put(rawSession.getId(), terminal);
            interviewByWs.put(rawSession.getId(), interviewId);
            log.info("Terminal WebSocket connected: wsId={}, interview={}", rawSession.getId(), interviewId);

        } catch (Exception e) {
            log.warn("Terminal session creation failed for interview {}: {}", interviewId, e.getMessage());
            sendErrorAndClose(rawSession, e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        TerminalSession terminal = terminalByWs.get(session.getId());
        if (terminal == null || !terminal.isAlive()) return;

        String payload = message.getPayload();
        if (payload.isEmpty()) return;

        char prefix = payload.charAt(0);
        String data = payload.substring(1);

        switch (prefix) {
            case 'i' -> {
                // 使用者按鍵輸入 → 寫入容器 stdin
                terminal.write(data.getBytes(StandardCharsets.UTF_8));
            }
            case 'r' -> {
                // 終端 resize：格式 r<cols>,<rows>
                String[] parts = data.split(",", 2);
                if (parts.length == 2) {
                    try {
                        int cols = Integer.parseInt(parts[0].trim());
                        int rows = Integer.parseInt(parts[1].trim());
                        terminal.resize(cols, rows);
                    } catch (NumberFormatException e) {
                        log.debug("Invalid resize message: {}", data);
                    }
                }
            }
            default -> log.debug("Unknown terminal message prefix '{}' from ws {}", prefix, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        TerminalSession terminal = terminalByWs.remove(session.getId());
        UUID interviewId = interviewByWs.remove(session.getId());

        if (terminal != null && interviewId != null) {
            terminalSessionService.removeSession(interviewId, terminal);
        }
        log.info("Terminal WebSocket closed: wsId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Terminal WebSocket transport error: wsId={}, error={}", session.getId(), exception.getMessage());
    }

    private void sendErrorAndClose(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage("e" + message));
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception e) {
            log.debug("Error sending error message to ws {}: {}", session.getId(), e.getMessage());
        }
    }
}
