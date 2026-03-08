package com.interview.ai.internal;

import com.google.genai.Client;
import com.interview.ai.AiModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
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
            double temperature = entry.temperature() != null ? entry.temperature() : 0.7;
            try {
                // 設計說明：以 switch 分派各 provider 的手動配置邏輯，避免使用 Spring Boot auto-config
                // starter，消除與 Spring Boot 4 的相容性衝突。每個 provider 各自建立 ChatModel 後統一
                // 包裝為 ChatClient，其餘服務層不需感知底層 provider 差異。
                switch (entry.provider()) {
                    case "google-genai" -> {
                        Client genAiClient = Client.builder()
                                .apiKey(entry.apiKey())
                                .build();
                        GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                                .genAiClient(genAiClient)
                                .defaultOptions(GoogleGenAiChatOptions.builder()
                                        .model(entry.id())
                                        .temperature(temperature)
                                        .build())
                                .build();
                        clients.put(entry.id(), ChatClient.create(chatModel));
                    }
                    case "anthropic" -> {
                        // 設計說明：使用 AnthropicApi + AnthropicChatModel builder 手動配置，
                        // 與 Google GenAI 相同模式，不依賴 spring-ai-anthropic-spring-boot-starter。
                        AnthropicApi anthropicApi = AnthropicApi.builder()
                                .apiKey(entry.apiKey())
                                .build();
                        AnthropicChatModel chatModel = AnthropicChatModel.builder()
                                .anthropicApi(anthropicApi)
                                .defaultOptions(AnthropicChatOptions.builder()
                                        .model(entry.id())
                                        .temperature(temperature)
                                        .build())
                                .build();
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

    public List<AiModelInfo> getAvailableModels() {
        return List.copyOf(availableModels);
    }

    public boolean isEmpty() {
        return clients.isEmpty();
    }

    public Optional<ChatClient> getFirstClient() {
        return clients.values().stream().findFirst();
    }
}
