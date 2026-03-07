package com.interview.ai.internal;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 對話記憶與 Advisor 配置。
 *
 * 使用 Spring AI 的 MessageWindowChatMemory 管理對話記憶視窗，
 * 底層透過 ConversationChatMemoryRepository 對應到既有 ai_conversations 表。
 *
 * maxMessages = 100：面試通常 60-120 分鐘，對話量約 30-100 則；
 * 啟用 ToolCallAdvisor 後，每輪 tool call 增加 2 條訊息（AssistantMessage + ToolResponseMessage），
 * 若對話密集使用工具，實際訊息數可能更多。Gemini 2.5 Flash 的 1M token context window 足以容納。
 *
 * Advisor 設計：
 * - MessageChatMemoryAdvisor：在每次 ChatClient 呼叫前從 ChatMemory 載入歷史，
 *   呼叫後儲存新訊息（含 tool 中間訊息）。
 * - ToolCallAdvisor：將 tool calling 循環從 ChatModel 內部移到 advisor chain，
 *   讓每輪 tool call 都經過 MessageChatMemoryAdvisor，中間訊息得以持久化到 DB。
 *   若不用 ToolCallAdvisor，ChatModel 內部循環完全繞過 advisor chain，
 *   中間的 tool 訊息永遠不會進入 ChatMemory，導致下輪對話 AI 不知道自己已讀過哪些檔案。
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
    MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        // 設計說明：conversationId 由呼叫端透過 advisor param 傳入（ChatMemory.CONVERSATION_ID）。
        // 搭配 ToolCallAdvisor 使用時，tool calling 每輪迭代都經過此 advisor，
        // 確保中間的 tool 訊息被持久化到 DB。
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    ToolCallAdvisor toolCallAdvisor() {
        // 設計說明：將 tool calling 循環從 ChatModel 內部移到 advisor chain。
        // 這樣每輪 tool call（readFile / listFiles / runCommand）都經過 MessageChatMemoryAdvisor，
        // 中間的 AssistantMessage(toolCalls) 和 ToolResponseMessage 得以持久化到 DB。
        // 下一輪對話時，AI 從 ChatMemory 取得完整歷史，知道自己已執行過哪些工具，
        // 不再重複說「我需要讀取檔案」。
        return ToolCallAdvisor.builder().build();
    }
}
