package com.interview.ai.internal;

import com.google.genai.Client;
import com.interview.ai.AiModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            if (entry.apiKey() == null || entry.apiKey().isBlank()) {
                log.warn("Skipping model '{}': no API key configured", entry.id());
                continue;
            }
            if (!"google-genai".equals(entry.provider())) {
                log.warn("Skipping model '{}': unsupported provider '{}'", entry.id(), entry.provider());
                continue;
            }
            try {
                Client genAiClient = Client.builder()
                        .apiKey(entry.apiKey())
                        .build();

                double temperature = entry.temperature() != null ? entry.temperature() : 0.7;

                GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                        .genAiClient(genAiClient)
                        .defaultOptions(GoogleGenAiChatOptions.builder()
                                .model(entry.id())
                                .temperature(temperature)
                                .build())
                        .build();

                clients.put(entry.id(), ChatClient.create(chatModel));
                availableModels.add(new AiModelInfo(entry.id(), entry.name(), entry.provider()));
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
