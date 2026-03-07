package com.interview.ai.application;

import com.interview.ai.internal.AciModelsProperties;
import com.interview.interview.InterviewStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
class AiSessionInitializer {

    private static final Logger log = LoggerFactory.getLogger(AiSessionInitializer.class);

    private final ChatMemory chatMemory;
    // 啟動時從 classpath 載入並快取，避免每次面試開始都重複 I/O
    private final String systemPrompt;

    AiSessionInitializer(ChatMemory chatMemory, AciModelsProperties properties,
                         ResourceLoader resourceLoader) throws IOException {
        this.chatMemory = chatMemory;
        String resourcePath = properties.systemPromptResource();
        Resource resource = resourceLoader.getResource(resourcePath);
        this.systemPrompt = resource.getContentAsString(StandardCharsets.UTF_8);
        log.info("AI system prompt loaded: resource={}, length={}", resourcePath, systemPrompt.length());
    }

    @EventListener
    public void onInterviewStarted(InterviewStartedEvent event) {
        try {
            // 使用 ChatMemory 介面寫入 SYSTEM prompt，與 Spring AI 官方設計保持一致
            chatMemory.add(event.interviewId().toString(), new SystemMessage(systemPrompt));
        } catch (Exception e) {
            log.error("Failed to initialize AI session for interview {}", event.interviewId(), e);
        }
    }
}
