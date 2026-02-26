package com.interview.monitoring.application;

import com.interview.ai.AiChatMessageEvent;
import com.interview.interview.CodeSubmittedEvent;
import com.interview.interview.InterviewCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InterviewMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(InterviewMonitoringService.class);

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID interviewId) {
        SseEmitter emitter = new SseEmitter(3_600_000L);

        emitters.computeIfAbsent(interviewId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(interviewId, emitter));
        emitter.onTimeout(() -> removeEmitter(interviewId, emitter));
        emitter.onError(e -> removeEmitter(interviewId, emitter));

        return emitter;
    }

    public void notifyCodeChange(UUID interviewId, String filePath, String code) {
        Map<String, Object> data = Map.of(
                "type", "code-change",
                "filePath", filePath != null ? filePath : "",
                "code", code != null ? code : ""
        );
        broadcast(interviewId, "code-change", data);
    }

    @EventListener
    public void onCodeSubmitted(CodeSubmittedEvent event) {
        String eventType = event.passed() ? "checkpoint-passed" : "checkpoint-failed";
        Map<String, Object> data = Map.of(
                "type", eventType,
                "checkpointId", event.checkpointId().toString(),
                "sequenceNumber", event.sequenceNumber(),
                "passed", event.passed()
        );
        broadcast(event.interviewId(), eventType, data);
    }

    @EventListener
    public void onAiChatMessage(AiChatMessageEvent event) {
        // 使用 MessageType enum 比對，比字串比對更安全
        String eventType = MessageType.USER == event.role() ? "ai-prompt-sent" : "ai-response-received";
        Map<String, Object> data = Map.of(
                "type", eventType,
                "role", event.role().name(),  // 轉為字串以符合前端期望格式
                "content", event.content()
        );
        broadcast(event.interviewId(), eventType, data);
    }

    @EventListener
    public void onInterviewCompleted(InterviewCompletedEvent event) {
        Map<String, Object> data = Map.of(
                "type", "interview-completed",
                "interviewId", event.interviewId().toString()
        );
        broadcast(event.interviewId(), "interview-completed", data);
    }

    private void broadcast(UUID interviewId, String eventName, Map<String, Object> data) {
        List<SseEmitter> targets = emitters.getOrDefault(interviewId, new CopyOnWriteArrayList<>());
        if (targets.isEmpty()) return;

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : targets) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                deadEmitters.add(emitter);
                log.debug("SSE emitter dead for interview {}, removing", interviewId);
            }
        }

        if (!deadEmitters.isEmpty()) {
            targets.removeAll(deadEmitters);
        }
    }

    private void removeEmitter(UUID interviewId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(interviewId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
