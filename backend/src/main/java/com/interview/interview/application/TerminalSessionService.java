package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.execution.TerminalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 管理每場面試的互動式終端 session 生命週期。
 *
 * 設計說明：
 * - 不依賴 InterviewService，避免循環依賴（InterviewService 在面試結束時呼叫本服務）。
 * - TerminalWebSocketHandler 負責解析面試 ID 和取得 containerId/workDir，
 *   本服務只負責 session 的建立、追蹤和清理。
 * - ConcurrentHashMap + CopyOnWriteArrayList 支援多執行緒並行建立/關閉 session。
 * - 每場面試最多 MAX_SESSIONS_PER_INTERVIEW 個並行 session，防止資源耗盡。
 */
@Service
public class TerminalSessionService {

    private static final Logger log = LoggerFactory.getLogger(TerminalSessionService.class);
    private static final int MAX_SESSIONS_PER_INTERVIEW = 5;

    private final ContainerService containerService;

    // interviewId → 該場面試的所有 active terminal sessions
    private final ConcurrentHashMap<UUID, List<TerminalSession>> sessionsByInterview
            = new ConcurrentHashMap<>();

    public TerminalSessionService(ContainerService containerService) {
        this.containerService = containerService;
    }

    /**
     * 為指定面試建立新的 terminal session。
     *
     * @param interviewId 面試 ID（用於追蹤和清理）
     * @param containerId 容器 ID（已確認在執行中）
     * @param workDir     bash 初始工作目錄（從 exam.yml 讀取）
     * @return 新建的 TerminalSession
     * @throws IllegalStateException 超過每場面試的 session 上限
     */
    public TerminalSession createSession(UUID interviewId, String containerId, String workDir) {
        List<TerminalSession> sessions = sessionsByInterview.computeIfAbsent(
                interviewId, k -> new CopyOnWriteArrayList<>());

        if (sessions.size() >= MAX_SESSIONS_PER_INTERVIEW) {
            throw new IllegalStateException(
                    "Too many terminal sessions for interview " + interviewId
                    + " (max " + MAX_SESSIONS_PER_INTERVIEW + ")");
        }

        TerminalSession session = containerService.createTerminalSession(containerId, workDir);
        sessions.add(session);
        log.info("Terminal session {} created for interview {} (total: {})",
                session.getId(), interviewId, sessions.size());
        return session;
    }

    /**
     * 移除並關閉單一 session（使用者關閉 tab 時呼叫）。
     */
    public void removeSession(UUID interviewId, TerminalSession session) {
        closeQuietly(session);
        List<TerminalSession> sessions = sessionsByInterview.get(interviewId);
        if (sessions != null) {
            sessions.remove(session);
            log.info("Terminal session {} removed for interview {}", session.getId(), interviewId);
        }
    }

    /**
     * 關閉並移除面試的所有 terminal sessions（面試完成或取消時呼叫）。
     */
    public void closeAllSessions(UUID interviewId) {
        List<TerminalSession> sessions = sessionsByInterview.remove(interviewId);
        if (sessions == null || sessions.isEmpty()) return;

        for (TerminalSession session : sessions) {
            closeQuietly(session);
        }
        log.info("Closed all {} terminal sessions for interview {}", sessions.size(), interviewId);
    }

    private void closeQuietly(TerminalSession session) {
        try {
            session.close();
        } catch (Exception e) {
            log.debug("Error closing terminal session {}: {}", session.getId(), e.getMessage());
        }
    }
}
