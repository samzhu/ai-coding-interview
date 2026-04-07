package com.interview.ai.internal;

import com.google.genai.Client;
import com.interview.ai.AiModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AiModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(AiModelRegistry.class);

    private final Map<String, ChatClient> clients = new LinkedHashMap<>();
    // 設計說明：User Controlled Tool Execution 需要直接呼叫 ChatModel.call(Prompt)，
    // 而 ChatClient 介面不暴露底層 ChatModel，因此額外儲存 ChatModel map 供 AiChatService 使用。
    private final Map<String, ChatModel> chatModels = new LinkedHashMap<>();
    private final List<AiModelInfo> availableModels = new ArrayList<>();

    AiModelRegistry(AciModelsProperties properties) {
        for (var entry : properties.models()) {
            // 設計說明：模型列表與 ChatClient 建立分離。所有 application.yml 配置的模型都會出現在
            // /api/v1/ai/models 回應中，讓前端永遠能顯示完整模型選單。
            // 沒有 API key 的模型仍可列出，但無法使用（getChatClient 回傳 empty）。
            availableModels.add(new AiModelInfo(entry.id(), entry.name(), entry.provider()));

            if (entry.apiKey() == null || entry.apiKey().isBlank()) {
                log.warn("Skipping model '{}': no API key configured", entry.id());
                continue;
            }
            try {
                // 設計說明：以 switch 分派各 provider 的手動配置邏輯，避免使用 Spring Boot auto-config
                // starter，消除與 Spring Boot 4 的相容性衝突。每個 provider 各自建立 ChatModel 後統一
                // 包裝為 ChatClient，其餘服務層不需感知底層 provider 差異。
                // temperature 不明確設定，讓各 provider 採用自身預設值。
                switch (entry.provider()) {
                    case "google-genai" -> {
                        // 設計說明：使用 PatchedGoogleGenAiChatModel 修復 Spring AI #5466。
                        // GoogleGenAiChatModel.Builder.build() 固定回傳父類別實例，無法透過 builder 取得子類別，
                        // 因此改用 constructor 直接建立 PatchedGoogleGenAiChatModel。
                        Client genAiClient = Client.builder()
                                .apiKey(entry.apiKey())
                                .build();
                        PatchedGoogleGenAiChatModel chatModel = new PatchedGoogleGenAiChatModel(
                                genAiClient,
                                GoogleGenAiChatOptions.builder()
                                        .model(entry.id())
                                        .build());
                        chatModels.put(entry.id(), chatModel);
                        clients.put(entry.id(), ChatClient.create(chatModel));
                    }
                    case "anthropic" -> {
                        // 設計說明：Spring AI 2.0.0-M3 起 Anthropic 整合改用官方 anthropic-java SDK，
                        // 原本的 AnthropicApi 已被移除。改為先以 AnthropicOkHttpClient.builder() 建立
                        // AnthropicClient，再透過 AnthropicChatModel.builder().anthropicClient(...) 注入。
                        // builder 的 defaultOptions 也已改名為 options。
                        AnthropicClient anthropicClient = AnthropicOkHttpClient.builder()
                                .apiKey(entry.apiKey())
                                .build();
                        AnthropicChatModel chatModel = AnthropicChatModel.builder()
                                .anthropicClient(anthropicClient)
                                .options(AnthropicChatOptions.builder()
                                        .model(entry.id())
                                        // 設計說明：Anthropic API 要求 max_tokens 必填，未設定會回 400 錯誤。
                                        // 16384 對應 Claude 3/4 系列的合理上限，足以容納完整面試回應。
                                        .maxTokens(16384)
                                        .build())
                                .build();
                        chatModels.put(entry.id(), chatModel);
                        clients.put(entry.id(), ChatClient.create(chatModel));
                    }
                    case "openai" -> {
                        // 設計說明：使用 OpenAiApi + OpenAiChatModel 手動配置，
                        // 與其他 provider 相同模式，不依賴 spring-ai-starter-model-openai。
                        OpenAiApi openAiApi = OpenAiApi.builder()
                                .apiKey(entry.apiKey())
                                .build();
                        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                                .openAiApi(openAiApi)
                                .defaultOptions(OpenAiChatOptions.builder()
                                        .model(entry.id())
                                        .build())
                                .build();
                        chatModels.put(entry.id(), chatModel);
                        clients.put(entry.id(), ChatClient.create(chatModel));
                    }
                    default -> {
                        log.warn("Skipping model '{}': unsupported provider '{}'", entry.id(), entry.provider());
                        continue;
                    }
                }
                log.info("Registered AI model: {} ({})", entry.id(), entry.provider());
            } catch (Exception e) {
                log.error("Failed to register model '{}': {}", entry.id(), e.getMessage());
            }
        }
        if (clients.isEmpty()) {
            log.warn("No AI models registered. AI features will be disabled.");
        } else {
            log.info("AI Model Registry initialized with {} model(s)", clients.size());
        }
    }

    public Optional<ChatClient> getChatClient(String modelId) {
        return Optional.ofNullable(clients.get(modelId));
    }

    public Optional<ChatModel> getChatModel(String modelId) {
        return Optional.ofNullable(chatModels.get(modelId));
    }

    public List<AiModelInfo> getAvailableModels() {
        return List.copyOf(availableModels);
    }

    public boolean isEmpty() {
        return clients.isEmpty();
    }

    public Optional<ChatClient> getFirstClient() {
        return clients.values().stream().findFirst();
    }

    public Optional<ChatModel> getFirstChatModel() {
        return chatModels.values().stream().findFirst();
    }
}
