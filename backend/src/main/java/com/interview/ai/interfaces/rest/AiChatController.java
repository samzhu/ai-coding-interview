package com.interview.ai.interfaces.rest;

import com.interview.ai.application.AiChatService;
import com.interview.ai.internal.EditProposal;
import com.interview.ai.internal.EditProposalParser;
import jakarta.validation.Valid;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/ai")
class AiChatController {

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

            // 同步累積完整回應，供串流結束後解析 edit_proposal 區塊
            StringBuilder fullResponse = new StringBuilder();

            try {
                // 建立 tool 事件 SSE 寫入器（Consumer<String>）。
                // 設計說明：工具執行時（listFiles / readFile / runCommand）會在各自執行緒上
                // 呼叫此 consumer，直接將 data-tool-invocation SSE 事件寫入 HTTP 串流。
                // 前端即時收到 "running" → "completed" 事件，無需等到文字開始串流才知道工具在運行。
                // 因工具執行與文字 delta 為嚴格順序（Flux.defer 中工具先完成才 emit 文字），
                // 不會發生並發寫入，此 consumer 不需要額外同步機制。
                Consumer<String> toolSseEmitter = (sseLine) -> {
                    try {
                        writer.write(sseLine);
                        writer.flush();
                    } catch (IOException e) {
                        // client disconnected — ignore silently
                    }
                };

                AiChatService.StreamingChatResult result = service.streamChat(interviewId, userMessage, request.modelId(), toolSseEmitter);

                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Throwable> errorRef = new AtomicReference<>();

                result.tokenStream()
                        .publishOn(Schedulers.boundedElastic())
                        .subscribe(
                                token -> {
                                    try {
                                        String deltaJson = objectMapper.writeValueAsString(
                                                Map.of("type", "text-delta", "id", textId.toString(), "delta", token));
                                        writer.write("data: " + deltaJson + "\n\n");
                                        writer.flush();
                                        fullResponse.append(token);
                                    } catch (Exception ignored) {
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
                    String errMsg = errorRef.get().getMessage() != null
                            ? errorRef.get().getMessage() : "AI 服務暫時不可用，請稍後再試";
                    String deltaJson = objectMapper.writeValueAsString(
                            Map.of("type", "text-delta", "id", textId.toString(), "delta", errMsg));
                    writer.write("data: " + deltaJson + "\n\n");
                    writer.flush();
                }

            } catch (Exception e) {
                // 業務例外（AI 停用、面試超時等）：送錯誤文字，避免 network error
                String errMsg = e.getMessage() != null ? e.getMessage() : "發生錯誤，請稍後再試";
                String deltaJson = objectMapper.writeValueAsString(
                        Map.of("type", "text-delta", "id", textId.toString(), "delta", errMsg));
                writer.write("data: " + deltaJson + "\n\n");
                writer.flush();
            }

            writer.write("data: {\"type\":\"text-end\",\"id\":\"" + textId + "\"}\n\n");
            writer.flush();

            // 解析 AI 回應中的 edit_proposal 區塊，以 data 事件傳至前端供候選人審查套用
            List<EditProposal> proposals = editProposalParser.parse(fullResponse.toString());
            for (EditProposal proposal : proposals) {
                String dataEvent = buildEditProposalDataEvent(proposal);
                writer.write("data: " + dataEvent + "\n\n");
                writer.flush();
            }
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
