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
// editProposal 回傳 "PENDING_REVIEW" 列舉狀態（proposalId 已由 ToolCall.id 攜帶在 memory 中），
// callLLMAndLoop() 偵測 editProposal → break loop，等待候選人回應。
// 候選人的 accept/reject 透過 /chat/stream 以 action:ACCEPTED:<proposalId> / action:REJECTED:<proposalId> 傳入，
// 由 AiChatService.handleAccepted/handleRejected 直接處理（不呼叫 LLM）。

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
        try {
            String content = fileProvider.readFile(interviewId, path);
            String result = (content != null && !content.isBlank()) ? content : "(empty file)";
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
        try {
            CodeExecutionResult result = fileProvider.execInWorkspace(interviewId, command, RUN_COMMAND_TIMEOUT_SECONDS);
            long elapsed = System.currentTimeMillis() - t0;
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
     * 新設計：
     * - 工具回傳 "PENDING_REVIEW"，callLLMAndLoop() 偵測到此結果後 break loop。
     * - break 後回傳固定文字「已提交修改建議」給候選人，不再呼叫 LLM 產生說明文字。
     * - proposalId 為 ToolCall.id（AI 模型產生），已存入 memory 的 ASSISTANT toolCalls。
     * - 候選人審查 diff 後，透過前端 sendMessage("action:ACCEPTED:<proposalId>") 或
     *   sendMessage("action:REJECTED:<proposalId>") 送入 /chat/stream，
     *   由 AiChatService.handleAccepted/handleRejected 直接處理（不呼叫 LLM）。
     */
    @Tool(description = "Submit batch code edit proposals for candidate review. " +
            "Gather all information using other tools FIRST, then call this ONCE. " +
            "'original' must match the current file content exactly (use readFile to get it). " +
            "After submitting proposals, explain the changes to the candidate in text. " +
            "The candidate will review the diff and send ACCEPTED or REJECTED.")
    public String editProposal(
            @ToolParam(description = "List of file changes to propose")
            List<FileChange> changes,
            ToolContext toolContext) {
        UUID interviewId = extractInterviewId(toolContext);

        // 設計說明：proposalId = ToolCall.id（AI 模型產生，已存在 memory ASSISTANT toolCalls 中）。
        // callLLMAndLoop() 在 cb.call() 前注入到 ToolContext 共享 HashMap。
        // handleAccepted() 收到此 ID 後，掃描 memory 中 ASSISTANT toolCalls 比對 tc.id() 即可找到 arguments。
        String proposalId = (String) toolContext.getContext().getOrDefault("currentToolCallId", "unknown");

        // 設計說明：editProposal 不發送 data-tool-invocation 事件。
        // ChangeSetCard 已透過 data-edit-proposal SSE 事件完整呈現修改內容，
        // 額外的 tool badge 只會讓前端顯示 "unknown editProposal"（多餘且令人困惑）。
        for (FileChange change : changes) {
            emitEditProposalEvent(toolContext, proposalId, change.file(), change.original(), change.proposed());
        }

        // 設計說明：回傳列舉狀態即可。proposalId 不需放在回傳值中——
        // 它已透過 ToolCall.id 存在 memory 的 ASSISTANT 訊息 toolCalls[] 裡，
        // handleAccepted() 用 proposalId 掃描 memory 即可精確定位。
        // 後端 callLLMAndLoop() 偵測 editProposal → break，候選人按同意/拒絕由 handleAccepted/handleRejected 處理。
        return "PENDING_REVIEW";
    }

    /**
     * 發送 data-edit-proposal SSE 事件給前端。
     * 格式與前端 AI SDK v6 UIMessageStreamParser 期望的格式一致。
     *
     * 設計說明：proposalId = ToolCall.id，由 callLLMAndLoop() 注入到 ToolContext，
     * 前端收到後存入 EditProposalData，供 Accept/Reject 時精確傳回後端定位 tool call arguments。
     */
    private void emitEditProposalEvent(ToolContext toolContext, String proposalId,
                                       String file, String original, String proposed) {
        Object rawEmitter = toolContext.getContext().get("toolSseEmitter");
        if (rawEmitter instanceof Consumer) {
            @SuppressWarnings("unchecked")
            Consumer<String> emitter = (Consumer<String>) rawEmitter;
            try {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("type", "edit-proposal");
                data.put("proposalId", proposalId != null ? proposalId : "unknown");
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
