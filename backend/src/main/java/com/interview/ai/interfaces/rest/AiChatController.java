package com.interview.ai.interfaces.rest;

import com.interview.ai.application.AiChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/ai")
class AiChatController {

    private final AiChatService service;

    AiChatController(AiChatService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    ChatResponse chat(@PathVariable UUID interviewId,
                      @RequestBody @Valid ChatRequest request) {
        return ChatResponse.from(service.chat(interviewId, request.message()));
    }

    @PostMapping(value = "/chat/stream")
    ResponseEntity<StreamingResponseBody> streamChat(@PathVariable UUID interviewId,
                                                     @RequestBody StreamChatRequest request) {
        String userMessage = request.extractLastUserMessage();
        UUID msgId = UUID.randomUUID();
        UUID textId = UUID.randomUUID();

        StreamingResponseBody body = outputStream -> {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            // SSE format: "data: JSON\n\n" — required by AI SDK DefaultChatTransport (uses EventSourceParserStream)
            writer.write("data: {\"type\":\"start\",\"messageId\":\"" + msgId + "\"}\n\n");
            writer.flush();
            writer.write("data: {\"type\":\"text-start\",\"id\":\"" + textId + "\"}\n\n");
            writer.flush();

            try {
                AiChatService.StreamingChatResult result = service.streamChat(interviewId, userMessage);

                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Throwable> errorRef = new AtomicReference<>();

                result.tokenStream()
                        .publishOn(Schedulers.boundedElastic())
                        .subscribe(
                                token -> {
                                    try {
                                        writer.write("data: {\"type\":\"text-delta\",\"id\":\"" + textId + "\",\"delta\":\"" + jsonEscape(token) + "\"}\n\n");
                                        writer.flush();
                                    } catch (IOException ignored) {
                                        // client disconnected — stop silently
                                        latch.countDown();
                                    }
                                },
                                error -> {
                                    errorRef.set(error);
                                    latch.countDown();
                                },
                                latch::countDown
                        );

                latch.await(60, TimeUnit.SECONDS);

                if (errorRef.get() != null) {
                    // Gemini 或其他非同步錯誤：送錯誤文字，不要直接關閉連線
                    String msg = jsonEscape(errorRef.get().getMessage() != null
                            ? errorRef.get().getMessage() : "AI 服務暫時不可用，請稍後再試");
                    writer.write("data: {\"type\":\"text-delta\",\"id\":\"" + textId + "\",\"delta\":\"" + msg + "\"}\n\n");
                    writer.flush();
                }

            } catch (Exception e) {
                // 業務例外（AI 停用、面試超時等）：送錯誤文字，避免 network error
                String msg = jsonEscape(e.getMessage() != null ? e.getMessage() : "發生錯誤，請稍後再試");
                writer.write("data: {\"type\":\"text-delta\",\"id\":\"" + textId + "\",\"delta\":\"" + msg + "\"}\n\n");
                writer.flush();
            }

            writer.write("data: {\"type\":\"text-end\",\"id\":\"" + textId + "\"}\n\n");
            writer.flush();
            writer.write("data: {\"type\":\"finish\"}\n\n");
            writer.flush();
            writer.write("data: [DONE]\n\n");
            writer.flush();
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

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
