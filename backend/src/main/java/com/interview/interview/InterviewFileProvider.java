package com.interview.interview;

import com.interview.execution.CodeExecutionResult;
import com.interview.execution.ContainerFile;

import java.util.List;
import java.util.UUID;

/**
 * interview 模組對外公開的工作區文件操作介面。
 *
 * 設計說明：
 * 此介面位於 interview 根套件，符合 Spring Modulith 模組邊界規範，
 * 讓 ai 模組可合法依賴 interview 模組的文件操作能力，
 * 而不需要直接存取 interview.application 子套件（模組內部實作）。
 */
public interface InterviewFileProvider {

    /** 列出工作區所有檔案（已套用 exam.yml 的 exclude 規則）。 */
    List<ContainerFile> listFiles(UUID interviewId);

    /** 讀取指定路徑的檔案內容（相對或絕對路徑皆可）。 */
    String readFile(UUID interviewId, String path);

    /**
     * 在工作區目錄下執行 shell 指令（自動 cd 至 workspace）。
     *
     * @param command        要執行的指令，例如 "python -m pytest test_main.py"
     * @param timeoutSeconds 最大執行秒數
     */
    CodeExecutionResult execInWorkspace(UUID interviewId, String command, int timeoutSeconds);
}
