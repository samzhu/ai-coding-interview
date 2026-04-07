package com.interview.monitoring.application;

import com.interview.ai.AiEditAcceptedEvent;
import com.interview.ai.AiEditProposalEvent;
import com.interview.ai.AiEditRejectedEvent;
import com.interview.monitoring.domain.ActivityEventRecord;
import com.interview.monitoring.infrastructure.ActivityEventJdbcRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 跨模組事件持久化：訂閱 ai 模組的 edit lifecycle events 寫入 interview_activity_events。
 *
 * 設計說明：用 @ApplicationModuleListener 而非 @EventListener，因為：
 *  - 異步執行，不阻塞 ai 模組的 publisher
 *  - 獨立 transaction (REQUIRES_NEW)
 *  - Spring Modulith Event Publication Registry 自動重試失敗寫入
 * SSE 廣播由另一個 InterviewMonitoringService 用 @EventListener 處理（fire-and-forget）。
 * 兩個 listener 各司其職：一個求即時、一個求可靠。
 * 來源：https://docs.spring.io/spring-modulith/reference/events.html
 */
@Component
public class MonitoringEventPersister {

    private final ActivityEventJdbcRepository repository;

    MonitoringEventPersister(ActivityEventJdbcRepository repository) {
        this.repository = repository;
    }

    @ApplicationModuleListener
    public void onAiEditProposal(AiEditProposalEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("proposalId", event.proposalId());
        payload.put("filePath", event.filePath());
        payload.put("diff", event.diff());
        repository.save(ActivityEventRecord.of(event.interviewId(), "ai.edit.proposal", payload));
    }

    @ApplicationModuleListener
    public void onAiEditAccepted(AiEditAcceptedEvent event) {
        repository.save(ActivityEventRecord.of(
                event.interviewId(), "ai.edit.accepted",
                Map.of("proposalId", event.proposalId())));
    }

    @ApplicationModuleListener
    public void onAiEditRejected(AiEditRejectedEvent event) {
        repository.save(ActivityEventRecord.of(
                event.interviewId(), "ai.edit.rejected",
                Map.of("proposalId", event.proposalId())));
    }
}
