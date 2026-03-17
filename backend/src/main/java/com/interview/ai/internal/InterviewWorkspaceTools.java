package com.interview.ai.internal;

import com.interview.ai.AiToolExecutionEvent;
import com.interview.execution.CodeExecutionResult;
import com.interview.execution.ContainerFile;
import com.interview.interview.InterviewFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ToolContext 路徑版本差異：
//   Spring AI 2.0.0-M2（目前使用）：org.springframework.ai.chat.model.ToolContext
//   Spring AI 2.0.0 GA（升級後）：  org.springframework.ai.tool.context.ToolContext
// 升級至 GA 版本時，需將下方 import 路徑改為 org.springframework.ai.tool.context.ToolContext
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// 設計說明：edit_proposal 改為 @Tool 函式（而非 system prompt 中描述的 XML 文字格式）。
// 原因：Gemini 模型會將 system prompt 中的 XML 格式誤認為可呼叫的 function call，
// 導致 IllegalStateException: No ToolCallback found for tool name: edit_proposal。
// 正式 @Tool 宣告讓 Spring AI 自動處理 JSON schema 與反序列化，不再依賴 regex 解析。
// editProposal 正常返回結果（不再使用 PENDING_REVIEW sentinel），
// 候選人的 accept/reject 透過 /chat/stream 以 action: 前綴傳入。

/**
 * Spring AI @Tool 工具集，供 AI 助手在對話過程中自動呼叫（tool calling）。
 *
 * 設計說明：
 * - 工具方法透過 ToolContext 取得 interviewId，避免 singleton bean 持有請求狀態
 * - 讀取工具（listFiles、readFile）和執行工具（runCommand）由 AI 自動呼叫
 * - 修改檔案刻意不提供 writeFile 工具；AI 透過 editProposal 工具提出修改建議，
 *   候選人審查後手動套用，確保「人在迴路中（Human-in-the-loop）」的控制權
 * - runCommand 限制 30 秒 timeout 防止長時間阻塞
 *
 * Tool 事件即時推送設計：
 * - ToolContext 攜帶 "toolSseEmitter" (Consumer<String>)，由 AiChatController 注入
 * - 每個工具執行前後呼叫 emitToolEvent()，直接寫入 SSE 串流
 * - 前端即時收到 "running" → "completed" 事件，顯示工具執行狀態卡片
 * - 由於工具在 Flux.defer() 同一執行緒中運行（tools → text 為嚴格順序），
 *   不會與文字 chunk 寫入發生競爭，無需額外同步
 */
@Component
public class InterviewWorkspaceTools {

    private static final Logger log = LoggerFactory.getLogger(InterviewWorkspaceTools.class);
    private static final int RUN_COMMAND_TIMEOUT_SECONDS = 30;

    private final InterviewFileProvider fileProvider;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public InterviewWorkspaceTools(InterviewFileProvider fileProvider, ObjectMapper objectMapper,
                                   ApplicationEventPublisher eventPublisher) {
        this.fileProvider = fileProvider;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 列出工作區所有檔案結構（已排除 exam.yml 等系統檔）。
     */
    @Tool(description = "List all files in the candidate's workspace project structure. Use this first to understand the project layout before reading files.")
    public String listFiles(ToolContext toolContext) {
        UUID interviewId = extractInterviewId(toolContext);
        String callId = UUID.randomUUID().toString();
        emitToolEvent(toolContext, callId, "listFiles", "running", null);
        log.info("[AI-DIAG] interview={} tool=listFiles invoked", interviewId);
        try {
            List<ContainerFile> files = fileProvider.listFiles(interviewId);
            String result;
            if (files.isEmpty()) {
                result = "Workspace is empty.";
            } else {
                result = files.stream()
                        .map(f -> (f.isDirectory() ? "[DIR]  " : "[FILE] ") + f.filePath())
                        .collect(Collectors.joining("\n"));
            }
            log.info("[AI-DIAG] interview={} tool=listFiles completed, files={}", interviewId, files.size());
            emitToolEvent(toolContext, callId, "listFiles", "completed",
                    files.stream().filter(f -> !f.isDirectory()).count() + " 個檔案");
            return result;
        } catch (Exception e) {
            log.warn("listFiles failed for interview {}", interviewId, e);
            emitToolEvent(toolContext, callId, "listFiles", "error", e.getMessage());
            return "Error listing files: " + e.getMessage();
        }
    }

    /**
     * 讀取指定路徑的檔案內容。
     */
    @Tool(description = "Read the content of a file in the workspace. Use relative path from workspace root, e.g. 'main.py' or 'src/App.java'.")
    public String readFile(
            @ToolParam(description = "Relative file path from workspace root, e.g. 'main.py' or 'tests/test_main.py'")
            String path,
            ToolContext toolContext) {
        UUID interviewId = extractInterviewId(toolContext);
        String callId = UUID.randomUUID().toString();
        // 顯示正在讀取的檔案路徑，讓使用者知道 AI 在看哪個檔案
        String shortPath = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        emitToolEvent(toolContext, callId, "readFile", "running", shortPath);
        log.info("[AI-DIAG] interview={} tool=readFile invoked, path={}", interviewId, path);
        try {
            String content = fileProvider.readFile(interviewId, path);
            String result = (content != null && !content.isBlank()) ? content : "(empty file)";
            log.info("[AI-DIAG] interview={} tool=readFile completed, path={} length={}",
                    interviewId, path, result.length());
            emitToolEvent(toolContext, callId, "readFile", "completed", shortPath);
            return result;
        } catch (Exception e) {
            log.warn("readFile '{}' failed for interview {}", path, interviewId, e);
            emitToolEvent(toolContext, callId, "readFile", "error", shortPath);
            return "Error reading file '" + path + "': " + e.getMessage();
        }
    }

    /**
     * 在工作區執行 shell 指令（例如跑測試、編譯）。
     * 指令自動在 workspace 目錄下執行，最長 30 秒。
     */
    @Tool(description = "Run a shell command in the candidate's workspace, e.g. run tests or compile code. Command executes from workspace root. 30-second timeout.")
    public String runCommand(
            @ToolParam(description = "Shell command to execute, e.g. 'python -m pytest test_main.py -v' or 'javac Main.java && java Main'")
            String command,
            ToolContext toolContext) {
        UUID interviewId = extractInterviewId(toolContext);
        String callId = UUID.randomUUID().toString();
        // 顯示完整指令讓使用者知道 AI 在執行什麼（超過 50 字元時截斷）
        String displayCmd = command.length() > 50 ? command.substring(0, 47) + "..." : command;
        emitToolEvent(toolContext, callId, "runCommand", "running", displayCmd);
        long t0 = System.currentTimeMillis();
        log.info("[AI-DIAG] interview={} tool=runCommand invoked, cmd={}", interviewId, displayCmd);
        try {
            CodeExecutionResult result = fileProvider.execInWorkspace(interviewId, command, RUN_COMMAND_TIMEOUT_SECONDS);
            long elapsed = System.currentTimeMillis() - t0;
            log.info("[AI-DIAG] interview={} tool=runCommand completed in {}ms, exitCode={} stdout={}chars stderr={}chars",
                    interviewId, elapsed, result.exitCode(),
                    result.stdout() != null ? result.stdout().length() : 0,
                    result.stderr() != null ? result.stderr().length() : 0);
            StringBuilder sb = new StringBuilder();
            if (result.stdout() != null && !result.stdout().isBlank()) {
                sb.append("STDOUT:\n").append(result.stdout().stripTrailing());
            }
            if (result.stderr() != null && !result.stderr().isBlank()) {
                if (!sb.isEmpty()) sb.append("\n\n");
                sb.append("STDERR:\n").append(result.stderr().stripTrailing());
            }
            sb.append("\nExit code: ").append(result.exitCode());
            emitToolEvent(toolContext, callId, "runCommand", "completed",
                    displayCmd + " (exit " + result.exitCode() + ")");
            return sb.toString();
        } catch (Exception e) {
            log.warn("runCommand '{}' failed for interview {}", command, interviewId, e);
            emitToolEvent(toolContext, callId, "runCommand", "error", displayCmd);
            return "Error running command: " + e.getMessage();
        }
    }

    /**
     * 設計說明：使用 List<FileChange> record 參數，Spring AI 自動產生 JSON Schema
     * 並反序列化模型輸出。避免 String changesJson 的雙重 JSON 編碼問題
     * （Gemini 會在 JSON string 尾部附加多餘文字導致解析失敗）。
     * 參考：https://docs.spring.io/spring-ai/reference/2.0/api/tools.html
     *
     * 簡化設計：
     * - 工具正常返回描述性字串，tool loop 不中斷。
     * - AI 看到 tool result 後自然產生說明文字（解釋修改內容）。
     * - 候選人審查 diff 後，透過前端 sendMessage("action:ACCEPTED") 或
     *   sendMessage("action:REJECTED") 送入 /chat/stream，由 AiChatService 攔截處理。
     */
    @Tool(description = "Submit batch code edit proposals for candidate review. " +
            "Gather all information using other tools FIRST, then call this ONCE. " +
            "'original' must match the current file content exactly (use readFile to get it). " +
            "After calling this tool, STOP using tools and explain the changes in text. " +
            "The candidate will review the diff and send ACCEPTED or REJECTED.")
    public String editProposal(
            @ToolParam(description = "List of file changes to propose")
            List<FileChange> changes,
            ToolContext toolContext) {
        UUID interviewId = extractInterviewId(toolContext);
        String callId = UUID.randomUUID().toString();
        emitToolEvent(toolContext, callId, "editProposal", "running", null);

        for (FileChange change : changes) {
            emitEditProposalEvent(toolContext, change.file(), change.original(), change.proposed());
        }

        int count = changes.size();
        emitToolEvent(toolContext, callId, "editProposal", "completed",
                count + " 個檔案，等待審查");
        log.info("[AI-DIAG] interview={} editProposal submitted {} files", interviewId, count);

        return "Proposed changes to " + count + " files. Awaiting candidate review.";
    }

    /**
     * 發送 data-edit-proposal SSE 事件給前端。
     * 格式與前端 AI SDK v6 UIMessageStreamParser 期望的格式一致。
     */
    private void emitEditProposalEvent(ToolContext toolContext, String file, String original, String proposed) {
        Object rawEmitter = toolContext.getContext().get("toolSseEmitter");
        if (rawEmitter instanceof Consumer) {
            @SuppressWarnings("unchecked")
            Consumer<String> emitter = (Consumer<String>) rawEmitter;
            try {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("type", "edit-proposal");
                data.put("filePath", file != null ? file : "");
                data.put("original", original != null ? original : "");
                data.put("proposed", proposed != null ? proposed : "");

                Map<String, Object> event = Map.of(
                        "type", "data-edit-proposal",
                        "id", UUID.randomUUID().toString(),
                        "data", data
                );
                String json = objectMapper.writeValueAsString(event);
                emitter.accept("data: " + json + "\n\n");
            } catch (Exception e) {
                log.debug("Failed to emit edit proposal SSE event: {}", e.getMessage());
            }
        }
    }

    /**
     * 發送 tool 執行狀態 SSE 事件至前端，並同時發布 Spring 應用事件供 Admin 監控接收。
     *
     * 設計說明：
     * 1. ToolContext 中的 "toolSseEmitter" 是 AiChatController 注入的 Consumer<String>，
     *    接受已格式化的 SSE 行（"data: {...}\n\n"），寫入候選人端 HTTP 串流。
     *    前端 AI SDK 解析後在 message.parts 加入 data-tool-invocation 部分。
     * 2. 同時 publishEvent(AiToolExecutionEvent)，讓 InterviewMonitoringService
     *    透過 @EventListener 廣播至 Admin 即時監控的 SSE 連線。
     *
     * state 值與前端 ToolInvocationBadge 對應：
     * - "running":   旋轉動畫，顯示正在執行中
     * - "completed": 綠色勾選，顯示完成摘要
     * - "error":     紅色叉，顯示錯誤訊息
     */
    private void emitToolEvent(ToolContext toolContext, String toolCallId,
                               String toolName, String state, String summary) {
        // 1. 寫入候選人端 SSE 串流
        Object rawEmitter = toolContext.getContext().get("toolSseEmitter");
        if (rawEmitter instanceof Consumer) {
            @SuppressWarnings("unchecked")
            Consumer<String> emitter = (Consumer<String>) rawEmitter;
            try {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("toolCallId", toolCallId);
                data.put("toolName", toolName);
                data.put("state", state);
                if (summary != null) data.put("summary", summary);

                Map<String, Object> event = Map.of(
                        "type", "data-tool-invocation",
                        "id", UUID.randomUUID().toString(),
                        "data", data
                );
                String json = objectMapper.writeValueAsString(event);
                emitter.accept("data: " + json + "\n\n");
            } catch (Exception e) {
                log.debug("Failed to emit tool SSE event for {}/{}: {}", toolName, state, e.getMessage());
            }
        }

        // 2. 發布 Spring 事件，供 Admin 即時監控接收
        try {
            UUID interviewId = extractInterviewId(toolContext);
            eventPublisher.publishEvent(new AiToolExecutionEvent(interviewId, toolCallId, toolName, state, summary));
        } catch (Exception e) {
            log.debug("Failed to publish AiToolExecutionEvent for {}/{}: {}", toolName, state, e.getMessage());
        }
    }

    private UUID extractInterviewId(ToolContext toolContext) {
        return UUID.fromString((String) toolContext.getContext().get("interviewId"));
    }
}
