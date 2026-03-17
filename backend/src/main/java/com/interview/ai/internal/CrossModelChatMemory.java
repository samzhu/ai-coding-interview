package com.interview.ai.internal;

import com.interview.ai.AiModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 設計說明：per-request ChatMemory 包裝器，負責兩件事：
 * 1. get()：根據目標 provider 一次性轉換訊息格式（canonical DB → provider-specific）
 * 2. add()：注入 model metadata 到 AssistantMessage 和 ToolResponseMessage（供 DB 記錄，便於診斷）
 *
 * 核心原則：DB 以 canonical 格式儲存完整關聯（tool call IDs、names、args、responses、
 * thoughtSignatures）。get() 時根據 targetProvider 做一次性轉換，不分步。
 * 不應有「孤兒移除」概念——callWithToolLoop() 保證 ASSISTANT(toolCalls) → TOOL 成對儲存；
 * 若因 MessageWindow 截斷導致邊界訊息不成對，由各 provider transformer 自行 fold 為純文字。
 *
 * Provider 格式差異：
 * - Claude（Anthropic）：唯一 ID [a-zA-Z0-9_-]+、嚴格 user/assistant 交替、不接受 thoughtSignatures
 * - Gemini（Google）：function name 作為配對依據、不用 ID、需保留 thoughtSignatures
 * - OpenAI：call_xxx 格式 ID、不需嚴格交替
 *
 * 非 Spring Bean — 輕量 per-request 物件。
 * Provider 資訊來自 application.yaml（aci.models[].provider），
 * 透過 AiModelRegistry 查詢，新增模型只需改配置。
 *
 * 參考：https://docs.spring.io/spring-ai/reference/2.0/api/chat-memory.html
 */
public class CrossModelChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(CrossModelChatMemory.class);

    private final ChatMemory delegate;
    private final String targetModelId;
    private final String targetProvider;

    public CrossModelChatMemory(ChatMemory delegate, AiModelRegistry registry, String targetModelId) {
        this.delegate = delegate;
        this.targetModelId = targetModelId;
        this.targetProvider = registry.getAvailableModels().stream()
                .filter(m -> m.id().equals(targetModelId))
                .map(AiModelInfo::provider)
                .findFirst()
                .orElse("unknown");
    }

    // === ChatMemory 介面 ===

    /**
     * 注入 model metadata 到 AssistantMessage 和 ToolResponseMessage 後持久化。
     * 讓 DB 記錄每條訊息來源的 model ID，供日後診斷使用。
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        delegate.add(conversationId, tagMessages(messages));
    }

    /**
     * 取得歷史訊息，根據 targetProvider 一次性轉換格式。
     * 各 provider transformer 自行處理未配對訊息（fold 為純文字），不另行移除孤兒。
     */
    @Override
    public List<Message> get(String conversationId) {
        List<Message> raw = delegate.get(conversationId);
        return transformForProvider(raw);
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }

    /** 降級取得：所有 tool call 歷史摺疊為純文字（供 1st call 失敗時 retry 使用） */
    public List<Message> getFolded(String conversationId) {
        return foldToolCalls(delegate.get(conversationId));
    }

    // === Model Metadata 注入 ===

    private List<Message> tagMessages(List<Message> messages) {
        return messages.stream().map(this::tagWithModel).toList();
    }

    private Message tagWithModel(Message message) {
        if (message instanceof AssistantMessage am) {
            return tagIfAssistant(am);
        }
        if (message instanceof ToolResponseMessage trm) {
            return tagToolResponseMessage(trm);
        }
        return message;
    }

    /**
     * 注入 model ID 到 AssistantMessage.metadata。
     * 若 builder 重建後丟失 tool calls（runtime 相容性問題），保留原始物件（model 欄位為 null）。
     */
    private Message tagIfAssistant(AssistantMessage am) {
        Map<String, Object> props = new HashMap<>(am.getMetadata());
        props.put("model", targetModelId);
        AssistantMessage tagged = AssistantMessage.builder()
                .content(am.getText() != null ? am.getText() : "")
                .toolCalls(am.getToolCalls())
                .media(am.getMedia())
                .properties(props)
                .build();
        if (am.hasToolCalls() && !tagged.hasToolCalls()) {
            log.warn("[AI-DIAG] model tagging lost tool calls, keeping original (model will be null in DB)");
            return am;
        }
        return tagged;
    }

    /**
     * 設計說明：注入 model ID 到 ToolResponseMessage metadata，
     * 讓 DB 記錄此 TOOL response 對應的 model，
     * 即使 MessageWindow 截斷 ASSISTANT(toolCalls)，
     * TOOL response 仍帶有完整上下文。
     */
    private Message tagToolResponseMessage(ToolResponseMessage trm) {
        Map<String, Object> meta = new HashMap<>(trm.getMetadata());
        meta.put("model", targetModelId);
        return ToolResponseMessage.builder()
                .responses(trm.getResponses())
                .metadata(meta)
                .build();
    }

    // === Provider 轉換 ===

    /**
     * 設計說明：根據 targetProvider 一次性轉換所有訊息。
     * 不分「清理孤兒」和「適配格式」兩步——每個 provider 的 transformer
     * 完整處理所有 message type，包括未配對訊息的 fold。
     */
    private List<Message> transformForProvider(List<Message> messages) {
        boolean hasToolCalls = messages.stream()
                .anyMatch(m -> m instanceof AssistantMessage am && am.hasToolCalls());
        if (!hasToolCalls) return messages;

        return switch (targetProvider) {
            case "anthropic" -> normalizeForClaude(messages);
            case "google-genai" -> preserveForGemini(messages);
            case "openai" -> normalizeForOpenAi(messages);
            default -> foldToolCalls(messages);
        };
    }

    private List<Message> normalizeForClaude(List<Message> messages) {
        try {
            return normalizeToolCalls(messages);
        } catch (Exception e) {
            log.warn("[AI-DIAG] normalizeToolCalls failed, falling back to foldToolCalls", e);
            return foldToolCalls(messages);
        }
    }

    // === Tool Call 正規化（Claude）===

    /**
     * 正規化 tool call ID（Claude 專用）：
     * 1. 配對 ASSISTANT(toolCalls) + ToolResponseMessage → 產生新 UUID ID（tc_xxx 格式，符合 Claude [a-zA-Z0-9_-]+ 規則）
     * 2. 清除 Gemini-specific thoughtSignatures（跨 provider 時無效）
     * 3. 未配對 ASSISTANT(toolCalls) → fold 為純文字 "[Used tool: xxx]"
     * 4. 孤立 ToolResponseMessage（ASSISTANT(toolCalls) 已被截斷）→ fold 為純文字 "[Tool xxx returned result]"
     * 5. 合併連續 ASSISTANT messages（Claude 要求 human/assistant 嚴格交替）
     */
    private List<Message> normalizeToolCalls(List<Message> messages) {
        List<Message> result = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg instanceof AssistantMessage am && am.hasToolCalls()
                    && i + 1 < messages.size()
                    && messages.get(i + 1) instanceof ToolResponseMessage trm) {
                // 配對成功：重映射 ID，移除 thoughtSignatures
                List<AssistantMessage.ToolCall> newTCs = new ArrayList<>();
                List<ToolResponseMessage.ToolResponse> newTRs = new ArrayList<>();
                List<AssistantMessage.ToolCall> origTCs = am.getToolCalls();
                List<ToolResponseMessage.ToolResponse> origTRs = trm.getResponses();
                for (int j = 0; j < origTCs.size(); j++) {
                    String newId = "tc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
                    AssistantMessage.ToolCall tc = origTCs.get(j);
                    newTCs.add(new AssistantMessage.ToolCall(newId, tc.type(), tc.name(), tc.arguments()));
                    if (j < origTRs.size()) {
                        ToolResponseMessage.ToolResponse tr = origTRs.get(j);
                        newTRs.add(new ToolResponseMessage.ToolResponse(newId, tr.name(), tr.responseData()));
                    }
                }
                // 重建 AssistantMessage（不含 thoughtSignatures，跨 provider 無效）
                result.add(AssistantMessage.builder()
                        .content(am.getText() != null ? am.getText() : "")
                        .toolCalls(newTCs)
                        .build());
                result.add(ToolResponseMessage.builder().responses(newTRs).build());
                i++; // skip ToolResponseMessage（已消耗）
            } else if (msg instanceof AssistantMessage am && am.hasToolCalls()) {
                // 未配對 ASSISTANT(toolCalls)（MessageWindow 截斷或對話邊界）→ fold 為純文字
                String summary = am.getToolCalls().stream()
                        .map(tc -> "[Used tool: " + tc.name() + "]")
                        .collect(Collectors.joining("\n"));
                mergeOrAddAssistant(result, summary);
            } else if (msg instanceof ToolResponseMessage trm) {
                // 孤立 ToolResponseMessage（ASSISTANT(toolCalls) 已被截斷）→ fold 為純文字
                String summary = trm.getResponses().stream()
                        .map(r -> "[Tool " + r.name() + " returned result]")
                        .collect(Collectors.joining("\n"));
                mergeOrAddAssistant(result, summary);
            } else {
                result.add(msg);
            }
        }
        return mergeConsecutiveAssistant(result);
    }

    // === Provider-Specific Transformers ===

    /**
     * 設計說明：Gemini 使用 function name 配對（name-based），完全不用 ID。
     * 保留 thoughtSignatures（Gemini thinking 模型 API 要求）。
     * 未配對訊息 fold 為純文字。
     * 參考：https://ai.google.dev/gemini-api/docs/thought-signatures
     */
    private List<Message> preserveForGemini(List<Message> messages) {
        List<Message> result = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg instanceof AssistantMessage am && am.hasToolCalls()) {
                if (i + 1 < messages.size() && messages.get(i + 1) instanceof ToolResponseMessage) {
                    // 配對成功：保留原始格式（name-based 配對，保留 thoughtSignatures）
                    result.add(am);
                    result.add(messages.get(i + 1));
                    i++;
                } else {
                    // 未配對：fold 為純文字
                    String summary = am.getToolCalls().stream()
                            .map(tc -> "[Used tool: " + tc.name() + "]")
                            .collect(Collectors.joining("\n"));
                    mergeOrAddAssistant(result, summary);
                }
            } else if (msg instanceof ToolResponseMessage trm) {
                // 孤立 ToolResponseMessage → fold 為純文字
                String summary = trm.getResponses().stream()
                        .map(r -> "[Tool " + r.name() + " returned result]")
                        .collect(Collectors.joining("\n"));
                mergeOrAddAssistant(result, summary);
            } else {
                result.add(msg);
            }
        }
        return result;
    }

    /**
     * 設計說明：OpenAI 使用 ID-based 配對（call_xxx 格式），保留原始 ID。
     * 不需嚴格交替規則。未配對訊息 fold 為純文字。
     */
    private List<Message> normalizeForOpenAi(List<Message> messages) {
        List<Message> result = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg instanceof AssistantMessage am && am.hasToolCalls()) {
                if (i + 1 < messages.size() && messages.get(i + 1) instanceof ToolResponseMessage) {
                    // 配對成功：保留原始 ID（call_xxx 格式已符合 OpenAI 規格）
                    result.add(am);
                    result.add(messages.get(i + 1));
                    i++;
                } else {
                    // 未配對：fold 為純文字
                    String summary = am.getToolCalls().stream()
                            .map(tc -> "[Used tool: " + tc.name() + "]")
                            .collect(Collectors.joining("\n"));
                    mergeOrAddAssistant(result, summary);
                }
            } else if (msg instanceof ToolResponseMessage trm) {
                // 孤立 ToolResponseMessage → fold 為純文字
                String summary = trm.getResponses().stream()
                        .map(r -> "[Tool " + r.name() + " returned result]")
                        .collect(Collectors.joining("\n"));
                mergeOrAddAssistant(result, summary);
            } else {
                result.add(msg);
            }
        }
        return result;
    }

    /** 將所有 tool calls 和孤立 TOOL responses 摺疊為純文字（降級策略 / default provider fallback） */
    private List<Message> foldToolCalls(List<Message> messages) {
        List<Message> result = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg instanceof AssistantMessage am && am.hasToolCalls()) {
                String summary = am.getToolCalls().stream()
                        .map(tc -> "[Used tool: " + tc.name() + "]")
                        .collect(Collectors.joining("\n"));
                if (i + 1 < messages.size() && messages.get(i + 1) instanceof ToolResponseMessage) {
                    i++;
                }
                mergeOrAddAssistant(result, summary);
            } else if (msg instanceof ToolResponseMessage trm) {
                // 設計說明：孤立 ToolResponseMessage 也應 fold，不應直接丟棄，避免遺失上下文
                String summary = trm.getResponses().stream()
                        .map(r -> "[Tool " + r.name() + " returned result]")
                        .collect(Collectors.joining("\n"));
                mergeOrAddAssistant(result, summary);
            } else {
                result.add(msg);
            }
        }
        return result;
    }

    /** 若 result 最後是 AssistantMessage（非 toolCalls），合併文字；否則新增 */
    private void mergeOrAddAssistant(List<Message> result, String text) {
        if (!result.isEmpty() && result.getLast() instanceof AssistantMessage lastAm
                && !lastAm.hasToolCalls()) {
            String merged = (lastAm.getText() != null ? lastAm.getText() : "") + "\n" + text;
            result.removeLast();
            result.add(AssistantMessage.builder().content(merged).build());
        } else {
            result.add(AssistantMessage.builder().content(text).build());
        }
    }

    /** 合併連續 AssistantMessage（非 toolCalls），Claude 要求 human/assistant 嚴格交替 */
    private List<Message> mergeConsecutiveAssistant(List<Message> messages) {
        List<Message> result = new ArrayList<>();
        for (Message msg : messages) {
            if (msg instanceof AssistantMessage am && !am.hasToolCalls()
                    && !result.isEmpty() && result.getLast() instanceof AssistantMessage lastAm
                    && !lastAm.hasToolCalls()) {
                String merged = (lastAm.getText() != null ? lastAm.getText() : "")
                        + "\n" + (am.getText() != null ? am.getText() : "");
                result.removeLast();
                result.add(AssistantMessage.builder().content(merged).build());
            } else {
                result.add(msg);
            }
        }
        return result;
    }
}
