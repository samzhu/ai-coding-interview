package com.interview.execution;

import java.util.List;

/**
 * 從容器內 exam.yml 讀取的考試配置。
 * exam.yml 是面試官準備映像檔時的必要約定，Spring Boot 從容器讀取後快取使用。
 */
public record ExamConfig(
        String workspace,
        List<String> exclude,
        List<ExamCheckpoint> checkpoints,
        String systemPrompt) {

    public record ExamCheckpoint(
            String id,
            String title,
            String description,
            String testCommand) {}

    public static final String CONFIG_FILENAME = "exam.yml";

    /** exam.yml 預設放在 /workspace 下 */
    public static final String DEFAULT_WORKSPACE = "/workspace";

    /** 取得 exam.yml 的完整路徑 */
    public String configPath() {
        String ws = workspace != null ? workspace : DEFAULT_WORKSPACE;
        return ws + "/" + CONFIG_FILENAME;
    }

    /** 有效的 workspace 路徑（null 時回傳預設值） */
    public String effectiveWorkspace() {
        return workspace != null ? workspace : DEFAULT_WORKSPACE;
    }
}
