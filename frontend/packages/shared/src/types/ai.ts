// AI module types — mirrors backend ChatResponse and ConversationHistoryResponse

export interface AiModelInfo {
  id: string;
  name: string;
  provider: string;
}

export interface ChatMessage {
  id: string;
  role: "USER" | "ASSISTANT" | "SYSTEM" | "TOOL_CALL";
  content: string;
  createdAt: string;
  model?: string | null;
  promptTokens?: number | null;
  completionTokens?: number | null;
}

export interface ConversationHistoryResponse {
  messages: ChatMessage[];
}

export interface ChatRequest {
  message: string;
}
