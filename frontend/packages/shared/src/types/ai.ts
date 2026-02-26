// AI module types — mirrors backend ChatResponse and ConversationHistoryResponse

export interface AiModelInfo {
  id: string;
  name: string;
  provider: string;
}

export interface ChatMessage {
  id: string;
  role: "USER" | "ASSISTANT" | "SYSTEM";
  content: string;
  createdAt: string;
}

export interface ConversationHistoryResponse {
  messages: ChatMessage[];
}

export interface ChatRequest {
  message: string;
}
