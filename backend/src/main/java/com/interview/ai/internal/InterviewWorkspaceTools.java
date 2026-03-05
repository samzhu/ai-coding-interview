package com.interview.ai.internal;

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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spring AI @Tool 工具集，供 AI 助手在對話過程中自動呼叫（tool calling）。
 *
 * 設計說明：
 * - 工具方法透過 ToolContext 取得 interviewId，避免 singleton bean 持有請求狀態
 * - 讀取工具（listFiles、readFile）和執行工具（runCommand）由 AI 自動呼叫
 * - 修改檔案刻意不提供 writeFile 工具；AI 透過 edit_proposal 格式提出修改建議，
 *   候選人審查後手動套用，確保「人在迴路中（Human-in-the-loop）」的控制權
 * - runCommand 限制 30 秒 timeout 防止長時間阻塞
 */
@Component
public class InterviewWorkspaceTools {

    private static final Logger log = LoggerFactory.getLogger(InterviewWorkspaceTools.class);
    private static final int RUN_COMMAND_TIMEOUT_SECONDS = 30;

    private final InterviewFileProvider fileProvider;

    public InterviewWorkspaceTools(InterviewFileProvider fileProvider) {
        this.fileProvider = fileProvider;
    }

    /**
     * 列出工作區所有檔案結構（已排除 exam.yml 等系統檔）。
     */
    @Tool(description = "List all files in the candidate's workspace project structure. Use this first to understand the project layout before reading files.")
    public String listFiles(ToolContext toolContext) {
        UUID interviewId = extractInterviewId(toolContext);
        try {
            List<ContainerFile> files = fileProvider.listFiles(interviewId);
            if (files.isEmpty()) {
                return "Workspace is empty.";
            }
            return files.stream()
                    .map(f -> (f.isDirectory() ? "[DIR]  " : "[FILE] ") + f.filePath())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("listFiles failed for interview {}: {}", interviewId, e.getMessage());
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
        try {
            String content = fileProvider.readFile(interviewId, path);
            return (content != null && !content.isBlank()) ? content : "(empty file)";
        } catch (Exception e) {
            log.warn("readFile '{}' failed for interview {}: {}", path, interviewId, e.getMessage());
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
        try {
            CodeExecutionResult result = fileProvider.execInWorkspace(interviewId, command, RUN_COMMAND_TIMEOUT_SECONDS);
            StringBuilder sb = new StringBuilder();
            if (result.stdout() != null && !result.stdout().isBlank()) {
                sb.append("STDOUT:\n").append(result.stdout().stripTrailing());
            }
            if (result.stderr() != null && !result.stderr().isBlank()) {
                if (!sb.isEmpty()) sb.append("\n\n");
                sb.append("STDERR:\n").append(result.stderr().stripTrailing());
            }
            sb.append("\nExit code: ").append(result.exitCode());
            return sb.toString();
        } catch (Exception e) {
            log.warn("runCommand '{}' failed for interview {}: {}", command, interviewId, e.getMessage());
            return "Error running command: " + e.getMessage();
        }
    }

    private UUID extractInterviewId(ToolContext toolContext) {
        return UUID.fromString((String) toolContext.getContext().get("interviewId"));
    }
}
