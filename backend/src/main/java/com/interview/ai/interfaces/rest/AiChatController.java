package com.interview.ai.interfaces.rest;

import com.interview.ai.application.AiChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/ai")
class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final AiChatService service;
    private final ObjectMapper objectMapper;

    AiChatController(AiChatService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/chat")
    ChatResponse chat(@PathVariable UUID interviewId,
                      @RequestBody @Valid ChatRequest request) {
        return ChatResponse.from(service.chat(interviewId, request.message()));
    }

    @PostMapping(value = "/chat/stream")
    ResponseEntity<StreamingResponseBody> streamChat(@PathVariable UUID interviewId,
                                                     @RequestBody StreamChatRequest request) {
        log.info("[AI-DIAG] interview={} streamChat request, modelId={}", interviewId, request.modelId());
        String userMessage = request.extractLastUserMessage();

        return buildSseResponse(interviewId,
                toolSseEmitter -> service.streamChat(interviewId, userMessage, request.modelId(), toolSseEmitter));
    }

    @GetMapping("/history")
    ConversationHistoryResponse getHistory(@PathVariable UUID interviewId) {
        return ConversationHistoryResponse.from(service.getHistory(interviewId), objectMapper);
    }

    /**
     * 設計說明：SSE 串流的共用建構方法。
     *
     * resultSupplier 接受 toolSseEmitter，返回 StreamingChatResult。
     * SseWriter 序列化寫入、heartbeat 保活、latch 等待、error 處理、finish/DONE。
     *
     * latch timeout 設為 5 分鐘（tool loop 最長預期執行時間的安全上限）。
     */
    private ResponseEntity<StreamingResponseBody> buildSseResponse(
            UUID interviewId,
            Function<Consumer<String>, AiChatService.StreamingChatResult> resultSupplier) {

        UUID msgId = UUID.randomUUID();
        UUID textId = UUID.randomUUID();

        StreamingResponseBody body = outputStream -> {
            // 設計說明：SseWriter 序列化所有 SSE 寫入，防止 heartbeat 執行緒與 Flux token 執行緒並發寫入導致串流損壞。
            SseWriter sseWriter = new SseWriter(outputStream);

            sseWriter.writeEvent("{\"type\":\"start\",\"messageId\":\"" + msgId + "\"}");
            // 設計說明：text-start 延遲到第一個 text-delta 前才發送。
            // 若在此處提前送出，data-tool-invocation 事件會被插入 text-start 與 text-delta 之間，
            // AI SDK v6 UIMessageStreamParser 遇到 data-* 事件時會結束當前 text part 上下文，
            // 導致後續 text-delta 無法附加到訊息中，造成文字回應消失。
            AtomicBoolean textStarted = new AtomicBoolean(false);

            // 設計說明：heartbeatScheduler 在 AI 處理期間（工具執行 + 模型推理）每 15 秒發送
            // SSE comment（": keepalive\n\n"）以防止 TCP idle timeout 或瀏覽器靜默斷線。
            // 包含 editProposal pending 等待期間（候選人審查 diff 時連線保持）。
            // 一旦第一個 text-delta 到達（AI 開始回應），立即取消 heartbeat。
            ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

            try {
                Consumer<String> toolSseEmitter = sseWriter::writeRaw;
                AiChatService.StreamingChatResult result = resultSupplier.apply(toolSseEmitter);

                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Throwable> errorRef = new AtomicReference<>();
                AtomicInteger deltaCount = new AtomicInteger(0);

                // 啟動 heartbeat：每 15 秒送一次 SSE comment，維持連線直到第一個 token 到達
                ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(
                        () -> sseWriter.writeComment("keepalive"),
                        15, 15, TimeUnit.SECONDS);

                result.tokenStream()
                        .publishOn(Schedulers.boundedElastic())
                        .subscribe(
                                token -> {
                                    // 第一個 token 到達：AI 已開始回應，不再需要 keepalive
                                    heartbeat.cancel(false);
                                    try {
                                        // 確保 text-start 在第一個 text-delta 之前送出（僅送一次）
                                        if (textStarted.compareAndSet(false, true)) {
                                            sseWriter.writeEvent("{\"type\":\"text-start\",\"id\":\"" + textId + "\"}");
                                        }
                                        deltaCount.incrementAndGet();
                                        String deltaJson = objectMapper.writeValueAsString(
                                                Map.of("type", "text-delta", "id", textId.toString(), "delta", token));
                                        sseWriter.writeEvent(deltaJson);
                                    } catch (Exception ignored) {
                                        log.debug("Token write failed (client disconnected): {}", ignored.getMessage());
                                        latch.countDown();
                                    }
                                },
                                error -> {
                                    heartbeat.cancel(false);
                                    log.error("AI stream error for interview {}", interviewId, error);
                                    errorRef.set(error);
                                    latch.countDown();
                                },
                                () -> {
                                    heartbeat.cancel(false);
                                    log.info("[AI-DIAG] interview={} stream done, deltas={}",
                                            interviewId, deltaCount.get());
                                    latch.countDown();
                                }
                        );

                // latch timeout 設為 5 分鐘：tool loop 最長預期執行時間的安全上限
                boolean completed = latch.await(5, TimeUnit.MINUTES);
                if (!completed) {
                    heartbeat.cancel(false);
                    log.warn("[AI-DIAG] interview={} TIMEOUT 5min, deltas={}",
                            interviewId, deltaCount.get());
                }

                if (errorRef.get() != null) {
                    Throwable err = errorRef.get();
                    String errMsg = err.getMessage() != null ? err.getMessage() : "AI 服務暫時不可用，請稍後再試";
                    if (textStarted.compareAndSet(false, true)) {
                        sseWriter.writeEvent("{\"type\":\"text-start\",\"id\":\"" + textId + "\"}");
                    }
                    String deltaJson = objectMapper.writeValueAsString(
                            Map.of("type", "text-delta", "id", textId.toString(), "delta", errMsg));
                    sseWriter.writeEvent(deltaJson);
                }

            } catch (Exception e) {
                // 業務例外（AI 停用、面試超時等）送錯誤文字避免 network error
                if (e instanceof com.interview.ai.application.AiDisabledException
                        || e instanceof com.interview.interview.InterviewExpiredException) {
                    log.warn("AI chat rejected for interview {}: {}", interviewId, e.getMessage());
                } else {
                    log.error("Unexpected error in AI stream for interview {}", interviewId, e);
                }
                String errMsg = e.getMessage() != null ? e.getMessage() : "發生錯誤，請稍後再試";
                if (textStarted.compareAndSet(false, true)) {
                    sseWriter.writeEvent("{\"type\":\"text-start\",\"id\":\"" + textId + "\"}");
                }
                String deltaJson = objectMapper.writeValueAsString(
                        Map.of("type", "text-delta", "id", textId.toString(), "delta", errMsg));
                sseWriter.writeEvent(deltaJson);
            } finally {
                heartbeatScheduler.shutdown();
            }

            // AI 只有 tool call 無文字時，text-start 尚未送出，需補送以維持協議完整性
            if (textStarted.compareAndSet(false, true)) {
                sseWriter.writeEvent("{\"type\":\"text-start\",\"id\":\"" + textId + "\"}");
            }
            sseWriter.writeEvent("{\"type\":\"text-end\",\"id\":\"" + textId + "\"}");

            sseWriter.writeEvent("{\"type\":\"finish\"}");
            sseWriter.writeRaw("data: [DONE]\n\n");
            log.info("[AI-DIAG] interview={} SSE closed", interviewId);
        };

        return ResponseEntity.ok()
                .header("x-vercel-ai-ui-message-stream", "v1")
                .header("x-accel-buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }

    /**
     * Thread-safe SSE 寫入器。
     *
     * 設計說明：heartbeat ScheduledExecutorService 與 Flux token 訂閱執行緒會並發呼叫寫入。
     * synchronized 確保每筆 SSE 訊息完整寫入後才釋放鎖，防止兩個執行緒交錯造成串流損壞。
     * volatile disconnected 旗標讓 heartbeat 在 client 斷線（IOException）後立即停止寫入。
     */
    private static class SseWriter {
        private final OutputStream outputStream;
        private volatile boolean disconnected = false;

        SseWriter(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        /** 寫入標準 SSE data 事件：data: {data}\n\n */
        synchronized void writeEvent(String data) {
            if (disconnected) return;
            try {
                outputStream.write(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                disconnected = true;
            }
        }

        /** 直接寫入原始字串（給 toolSseEmitter 使用，已含完整 SSE 格式） */
        synchronized void writeRaw(String raw) {
            if (disconnected) return;
            try {
                outputStream.write(raw.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                disconnected = true;
            }
        }

        /** 寫入 SSE comment（heartbeat 用）：: {comment}\n\n */
        synchronized void writeComment(String comment) {
            if (disconnected) return;
            try {
                outputStream.write((":" + comment + "\n\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                disconnected = true;
            }
        }
    }

}
