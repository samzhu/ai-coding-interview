package com.interview.ai.interfaces.rest;

import com.interview.ai.application.AiChatService;
import com.interview.ai.internal.EditProposal;
import com.interview.ai.internal.EditProposalParser;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/ai")
class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final AiChatService service;
    private final EditProposalParser editProposalParser;
    private final ObjectMapper objectMapper;

    AiChatController(AiChatService service, EditProposalParser editProposalParser, ObjectMapper objectMapper) {
        this.service = service;
        this.editProposalParser = editProposalParser;
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
        UUID msgId = UUID.randomUUID();
        UUID textId = UUID.randomUUID();

        StreamingResponseBody body = outputStream -> {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            // 設計說明：SseWriter 序列化所有 SSE 寫入，防止 heartbeat 執行緒與 Flux token 執行緒並發寫入導致串流損壞。
            // volatile disconnected 旗標在 IOException 後讓 heartbeat 停止嘗試寫入。
            SseWriter sseWriter = new SseWriter(writer);

            // SSE format: "data: JSON\n\n" — required by AI SDK DefaultChatTransport (uses EventSourceParserStream)
            sseWriter.writeEvent("{\"type\":\"start\",\"messageId\":\"" + msgId + "\"}");
            sseWriter.writeEvent("{\"type\":\"text-start\",\"id\":\"" + textId + "\"}");

            // 同步累積完整回應，供串流結束後解析 edit_proposal 區塊
            StringBuilder fullResponse = new StringBuilder();

            // 設計說明：heartbeatScheduler 在 AI 處理期間（工具執行 + Gemini 推理）每 15 秒發送
            // SSE comment（": keepalive\n\n"）以防止 TCP idle timeout 或瀏覽器靜默斷線。
            // 一旦第一個 text-delta 到達（AI 開始回應），立即取消 heartbeat，避免不必要的寫入。
            ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

            try {
                // 建立 tool 事件 SSE 寫入器（Consumer<String>）。
                // 設計說明：工具執行時（listFiles / readFile / runCommand）會在各自執行緒上
                // 呼叫此 consumer，直接將 data-tool-invocation SSE 事件寫入 HTTP 串流。
                // 前端即時收到 "running" → "completed" 事件，無需等到文字開始串流才知道工具在運行。
                // 透過 SseWriter 序列化寫入，確保與 heartbeat 執行緒安全。
                Consumer<String> toolSseEmitter = sseWriter::writeRaw;

                AiChatService.StreamingChatResult result = service.streamChat(interviewId, userMessage, request.modelId(), toolSseEmitter);

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
                                        deltaCount.incrementAndGet();
                                        String deltaJson = objectMapper.writeValueAsString(
                                                Map.of("type", "text-delta", "id", textId.toString(), "delta", token));
                                        sseWriter.writeEvent(deltaJson);
                                        fullResponse.append(token);
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
                                    log.info("[AI-DIAG] interview={} stream done, deltas={} length={}",
                                            interviewId, deltaCount.get(), fullResponse.length());
                                    latch.countDown();
                                }
                        );

                // latch timeout 設為比 Flux timeout（90s）多 30s，確保 Flux timeout 先觸發並正確通知前端
                boolean completed = latch.await(120, TimeUnit.SECONDS);
                if (!completed) {
                    heartbeat.cancel(false);
                    log.warn("[AI-DIAG] interview={} TIMEOUT 120s, deltas={} length={}",
                            interviewId, deltaCount.get(), fullResponse.length());
                }

                if (errorRef.get() != null) {
                    // Gemini 或其他非同步錯誤：送錯誤文字，不要直接關閉連線
                    // TimeoutException 顯示友善訊息（避免 stack trace 暴露給前端）
                    Throwable err = errorRef.get();
                    String errMsg;
                    if (err instanceof java.util.concurrent.TimeoutException) {
                        log.warn("[AI-DIAG] interview={} AI call timeout after 90s", interviewId);
                        errMsg = "AI 回應逾時（90 秒），可能因對話歷史過長或模型負載過高。請縮短問題或稍後再試。";
                    } else {
                        errMsg = err.getMessage() != null ? err.getMessage() : "AI 服務暫時不可用，請稍後再試";
                    }
                    String deltaJson = objectMapper.writeValueAsString(
                            Map.of("type", "text-delta", "id", textId.toString(), "delta", errMsg));
                    sseWriter.writeEvent(deltaJson);
                }

            } catch (Exception e) {
                // 業務例外（AI 停用、面試超時等）送錯誤文字避免 network error；其餘例外記錄完整 stack trace
                if (e instanceof com.interview.ai.application.AiDisabledException
                        || e instanceof com.interview.interview.InterviewExpiredException) {
                    log.warn("AI chat rejected for interview {}: {}", interviewId, e.getMessage());
                } else {
                    log.error("Unexpected error in AI stream for interview {}", interviewId, e);
                }
                String errMsg = e.getMessage() != null ? e.getMessage() : "發生錯誤，請稍後再試";
                String deltaJson = objectMapper.writeValueAsString(
                        Map.of("type", "text-delta", "id", textId.toString(), "delta", errMsg));
                sseWriter.writeEvent(deltaJson);
            } finally {
                heartbeatScheduler.shutdown();
            }

            sseWriter.writeEvent("{\"type\":\"text-end\",\"id\":\"" + textId + "\"}");

            // 解析 AI 回應中的 edit_proposal 區塊，以 data 事件傳至前端供候選人審查套用
            List<EditProposal> proposals = editProposalParser.parse(fullResponse.toString());
            log.info("[AI-DIAG] interview={} editProposals={}", interviewId, proposals.size());
            for (EditProposal proposal : proposals) {
                String dataEvent = buildEditProposalDataEvent(proposal);
                sseWriter.writeEvent(dataEvent);
            }
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

    @GetMapping("/history")
    ConversationHistoryResponse getHistory(@PathVariable UUID interviewId) {
        return ConversationHistoryResponse.from(service.getHistory(interviewId));
    }

    /**
     * Thread-safe SSE 寫入器。
     *
     * 設計說明：heartbeat ScheduledExecutorService 與 Flux token 訂閱執行緒會並發呼叫寫入。
     * synchronized 確保每筆 SSE 訊息完整寫入後才釋放鎖，防止兩個執行緒交錯造成串流損壞。
     * volatile disconnected 旗標讓 heartbeat 在 client 斷線（IOException）後立即停止寫入。
     */
    private static class SseWriter {
        private final BufferedWriter writer;
        private volatile boolean disconnected = false;

        SseWriter(BufferedWriter writer) {
            this.writer = writer;
        }

        /** 寫入標準 SSE data 事件：data: {data}\n\n */
        synchronized void writeEvent(String data) {
            if (disconnected) return;
            try {
                writer.write("data: " + data + "\n\n");
                writer.flush();
            } catch (IOException e) {
                disconnected = true;
            }
        }

        /** 直接寫入原始字串（給 toolSseEmitter 使用，已含完整 SSE 格式） */
        synchronized void writeRaw(String raw) {
            if (disconnected) return;
            try {
                writer.write(raw);
                writer.flush();
            } catch (IOException e) {
                disconnected = true;
            }
        }

        /** 寫入 SSE comment（heartbeat 用）：: {comment}\n\n */
        synchronized void writeComment(String comment) {
            if (disconnected) return;
            try {
                writer.write(": " + comment + "\n\n");
                writer.flush();
            } catch (IOException e) {
                disconnected = true;
            }
        }
    }

    /**
     * 將 EditProposal 序列化為符合 AI SDK v6 UI Message Stream Protocol 的 data 事件 JSON。
     *
     * 設計說明：AI SDK v6 要求 data 事件的 type 必須以 "data-" 開頭（格式：data-${NAME}）。
     * 舊格式 {"type":"data","data":[...]} 已不被支援，SDK 會直接忽略（isDataUIMessageChunk
     * 以 chunk.type.startsWith("data-") 判斷）。
     * 正確格式：{"type":"data-edit-proposal","id":"...","data":{...single object...}}
     * 前端收到後，message.parts 會加入 { type: "data-edit-proposal", id: "...", data: {...} }。
     * 使用 ObjectMapper 序列化以確保所有特殊字元（含 Unicode 控制字元）均正確跳脫。
     */
    private String buildEditProposalDataEvent(EditProposal proposal) {
        UUID dataId = UUID.randomUUID();
        Map<String, Object> data = Map.of(
                "type", "edit-proposal",
                "filePath", proposal.filePath() != null ? proposal.filePath() : "",
                "original", proposal.original() != null ? proposal.original() : "",
                "proposed", proposal.proposed() != null ? proposal.proposed() : ""
        );
        Map<String, Object> event = Map.of(
                "type", "data-edit-proposal",
                "id", dataId.toString(),
                "data", data
        );
        return objectMapper.writeValueAsString(event);
    }
}
