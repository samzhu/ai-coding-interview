package com.interview.ai.internal;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 對話記憶配置。
 *
 * 使用 Spring AI 的 MessageWindowChatMemory 管理對話記憶視窗，
 * 底層透過 ConversationChatMemoryRepository 對應到既有 ai_conversations 表。
 *
 * maxMessages = 100：面試通常 60-120 分鐘，對話量約 30-100 則；
 * Gemini 2.5 Flash 的 1M token context window 足以容納 100 則對話。
 */
@Configuration
class AiMemoryConfig {

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(100)
                .build();
    }
}
