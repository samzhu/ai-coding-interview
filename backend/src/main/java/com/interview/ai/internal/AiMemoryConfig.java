package com.interview.ai.internal;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 對話記憶與工具執行配置。
 *
 * 使用 Spring AI 的 MessageWindowChatMemory 管理對話記憶視窗，
 * 底層透過 ConversationChatMemoryRepository 對應到既有 ai_conversations 表。
 *
 * maxMessages = 100：面試通常 60-120 分鐘，對話量約 30-100 則；
 * User Controlled Tool Execution 模式下，每輪 tool call 增加 2 條訊息
 * （AssistantMessage + ToolResponseMessage），若對話密集使用工具，實際訊息數可能更多。
 * Gemini 2.5 Flash 的 1M token context window 足以容納。
 *
 * 設計說明：改用 User Controlled Tool Execution（手動 tool loop + 手動 ChatMemory 管理），
 * 取代原本的 MessageChatMemoryAdvisor + ToolCallAdvisor 方案。
 * 官方文件明確指出 advisor chain 無法可靠持久化 tool 中間訊息。
 * 參考：https://docs.spring.io/spring-ai/reference/api/tools.html#_user_controlled_tool_execution
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

    @Bean
    ToolCallingManager toolCallingManager() {
        // 設計說明：DefaultToolCallingManager 負責執行 @Tool 方法並建立 ToolResponseMessage，
        // 供 AiChatService.callWithToolLoop() 在手動 tool loop 中使用。
        return ToolCallingManager.builder().build();
    }
}
