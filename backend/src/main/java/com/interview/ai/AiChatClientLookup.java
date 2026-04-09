package com.interview.ai;

import org.springframework.ai.chat.client.ChatClient;

import java.util.Optional;

/**
 * 設計說明：scoring 模組需要呼叫特定 model id (e.g. gemini-2.5-pro) 的 ChatClient
 * 做 holistic 評分。AiModelRegistry 在 ai 模組的 internal 子套件，跨模組無法直接 inject。
 * 此介面放在 ai 模組根套件作為公開查找 API，由 AiModelRegistry 實作。
 *
 * 為何用 Optional：模型可能未配置 API key 而被 skipped (見 AiModelRegistry 構造邏輯)，
 * caller 必須處理「找不到」的情境。
 */
public interface AiChatClientLookup {

    Optional<ChatClient> findByModelId(String modelId);
}
