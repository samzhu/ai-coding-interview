package com.interview.ai.application;

import com.interview.ai.internal.AciModelsProperties;
import com.interview.interview.InterviewFileProvider;
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
    private final InterviewFileProvider fileProvider;
    // 啟動時從 classpath 載入並快取，避免每次面試開始都重複 I/O
    private final String defaultSystemPrompt;

    AiSessionInitializer(ChatMemory chatMemory, InterviewFileProvider fileProvider,
                         AciModelsProperties properties, ResourceLoader resourceLoader) throws IOException {
        this.chatMemory = chatMemory;
        this.fileProvider = fileProvider;
        String resourcePath = properties.systemPromptResource();
        Resource resource = resourceLoader.getResource(resourcePath);
        this.defaultSystemPrompt = resource.getContentAsString(StandardCharsets.UTF_8);
        log.info("AI default system prompt loaded: resource={}, length={}", resourcePath, defaultSystemPrompt.length());
    }

    @EventListener
    public void onInterviewStarted(InterviewStartedEvent event) {
        try {
            // 優先使用 exam.yml 自訂 system prompt；無自訂時 fallback 到平台預設
            // 這讓不同題目的 Docker image 可以提供專屬 AI 行為指引
            String examPrompt = fileProvider.getExamSystemPrompt(event.interviewId());
            String prompt = examPrompt != null ? examPrompt : defaultSystemPrompt;
            if (examPrompt != null) {
                log.info("Using exam.yml system prompt for interview {}, length={}", event.interviewId(), examPrompt.length());
            }
            // 使用 ChatMemory 介面寫入 SYSTEM prompt，與 Spring AI 官方設計保持一致
            chatMemory.add(event.interviewId().toString(), new SystemMessage(prompt));
        } catch (Exception e) {
            log.error("Failed to initialize AI session for interview {}", event.interviewId(), e);
        }
    }
}
