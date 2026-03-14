package com.interview.ai.internal;

import com.google.genai.Client;
import com.google.genai.types.Candidate;
import com.google.genai.types.FinishReason;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.Part;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 設計說明：修復 Spring AI #5466（allMatch bug），讓 Gemini 3 thinking 模型的 tool call 能正確運作。
 *
 * <p>Gemini 3 模型（如 gemini-3.1-flash-lite-preview）的 thinking 無法完全關閉：
 * <ul>
 *   <li>thinkingBudget(0) 對 Gemini 3 無效，Gemini 3 使用 thinkingLevel（最低為 minimal）。</li>
 *   <li>Thinking 模式下，回應的 parts 為混合型別：[thought_part, functionCall_part]。</li>
 *   <li>原始程式碼（line 655）用 allMatch 判斷是否為 function call，混合回應時 allMatch = false，
 *       導致 tool calls 被丟棄。</li>
 *   <li>下一次請求中 function call 缺少 thought_signature → Gemini API 回 400 錯誤。</li>
 * </ul>
 *
 * <p>修復方式（參考 <a href="https://github.com/spring-projects/spring-ai/pull/5489">Spring AI PR #5489</a>）：
 * <ul>
 *   <li>allMatch → anyMatch：只要有任意 functionCall part 就視為 function call 回應。</li>
 *   <li>同時提取非 function call 的 text parts，合併為 AssistantMessage 的 content。</li>
 *   <li>既有的 thought_signature 提取（lines 638-647）與重新附加邏輯（lines 287-321）不動，繼續正確運作。</li>
 * </ul>
 *
 * <p>日後移除條件：Spring AI 發布包含 PR #5468 修復的版本後（預計 2.0.0-M3 或更高），可：
 * <ol>
 *   <li>移除此檔案。</li>
 *   <li>AiModelRegistry 改回使用 GoogleGenAiChatModel.builder()。</li>
 * </ol>
 *
 * <p>參考來源：
 * <ul>
 *   <li><a href="https://github.com/spring-projects/spring-ai/issues/5466">Spring AI Issue #5466</a></li>
 *   <li><a href="https://github.com/spring-projects/spring-ai/pull/5489">Spring AI PR #5489</a></li>
 *   <li><a href="https://ai.google.dev/gemini-api/docs/thinking">Gemini Thinking docs</a></li>
 *   <li><a href="https://ai.google.dev/gemini-api/docs/thought-signatures">Thought Signatures docs</a></li>
 * </ul>
 */
class PatchedGoogleGenAiChatModel extends GoogleGenAiChatModel {

    PatchedGoogleGenAiChatModel(Client genAiClient, GoogleGenAiChatOptions defaultOptions) {
        // 設計說明：使用與 GoogleGenAiChatModel.Builder.build() 相同的預設值：
        // ToolCallingManager.builder().build()、RetryUtils.DEFAULT_RETRY_TEMPLATE、ObservationRegistry.NOOP。
        super(genAiClient, defaultOptions,
                ToolCallingManager.builder().build(),
                RetryUtils.DEFAULT_RETRY_TEMPLATE,
                ObservationRegistry.NOOP);
    }

    /**
     * Override responseCandidateToGeneration to fix the allMatch → anyMatch bug.
     *
     * <p>原始方法（line 655-656）：
     * <pre>{@code
     * boolean isFunctionCall = ... .allMatch(part -> part.functionCall().isPresent());
     * }</pre>
     *
     * <p>修復後：
     * <pre>{@code
     * boolean hasFunctionCall = ... .anyMatch(part -> part.functionCall().isPresent());
     * }</pre>
     *
     * <p>混合回應（[thought_part, functionCall_part]）時，同時提取 text content 與 tool calls，
     * 組合為單一 AssistantMessage，保留 thoughtSignatures metadata 讓父類別的重新附加邏輯正常運作。
     */
    @Override
    protected List<Generation> responseCandidateToGeneration(Candidate candidate) {
        int candidateIndex = candidate.index().orElse(0);
        FinishReason candidateFinishReason = candidate.finishReason().orElse(new FinishReason(FinishReason.Known.STOP));

        Map<String, Object> messageMetadata = new HashMap<>();
        messageMetadata.put("candidateIndex", candidateIndex);
        messageMetadata.put("finishReason", candidateFinishReason);

        // 提取 thought signatures，供後續請求重新附加到 functionCall parts。
        // 邏輯與父類別 lines 638-647 相同，確保 thought_signature 正確傳遞。
        if (candidate.content().isPresent() && candidate.content().get().parts().isPresent()) {
            List<Part> parts = candidate.content().get().parts().get();
            List<byte[]> thoughtSignatures = parts.stream()
                    .filter(part -> part.thoughtSignature().isPresent())
                    .map(part -> part.thoughtSignature().get())
                    .toList();
            if (!thoughtSignatures.isEmpty()) {
                messageMetadata.put("thoughtSignatures", thoughtSignatures);
            }
        }

        ChatGenerationMetadata chatGenerationMetadata = ChatGenerationMetadata.builder()
                .finishReason(candidateFinishReason.toString())
                .build();

        if (candidate.content().isEmpty() || candidate.content().get().parts().isEmpty()) {
            AssistantMessage assistantMessage = AssistantMessage.builder()
                    .content("")
                    .properties(messageMetadata)
                    .build();
            return List.of(new Generation(assistantMessage, chatGenerationMetadata));
        }

        List<Part> parts = candidate.content().get().parts().get();

        // 修復：anyMatch 取代 allMatch，混合回應（thinking + function call）時正確識別 tool call。
        boolean hasFunctionCall = parts.stream().anyMatch(part -> part.functionCall().isPresent());

        if (hasFunctionCall) {
            // 提取非 function call 的 text parts（通常是 thinking summary text），合併為 content。
            String textContent = parts.stream()
                    .filter(part -> part.functionCall().isEmpty() && part.text().isPresent())
                    .map(part -> part.text().get())
                    .collect(Collectors.joining("\n"));

            // 提取 function call parts 轉為 ToolCall 物件。
            List<AssistantMessage.ToolCall> assistantToolCalls = parts.stream()
                    .filter(part -> part.functionCall().isPresent())
                    .map(part -> {
                        FunctionCall functionCall = part.functionCall().get();
                        String functionName = functionCall.name().orElse("");
                        String functionArguments = mapToJson(functionCall.args().orElse(Map.of()));
                        return new AssistantMessage.ToolCall("", "function", functionName, functionArguments);
                    })
                    .toList();

            AssistantMessage assistantMessage = AssistantMessage.builder()
                    .content(textContent)
                    .toolCalls(assistantToolCalls)
                    .properties(messageMetadata)
                    .build();

            return List.of(new Generation(assistantMessage, chatGenerationMetadata));
        }
        else {
            // 純文字回應：與父類別 else 分支相同邏輯。
            return parts.stream()
                    .map(part -> AssistantMessage.builder()
                            .content(part.text().orElse(""))
                            .properties(messageMetadata)
                            .build())
                    .map(assistantMessage -> new Generation(assistantMessage, chatGenerationMetadata))
                    .toList();
        }
    }

    /**
     * 複製自父類別 private mapToJson()（lines 401-408），因父類別宣告為 private static 無法繼承。
     * 功能：序列化 function call args Map 為 JSON string。
     */
    private static String mapToJson(Map<String, Object> map) {
        try {
            return ModelOptionsUtils.OBJECT_MAPPER.writeValueAsString(map);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to convert map to JSON", e);
        }
    }
}
