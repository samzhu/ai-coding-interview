"use client";

import { useChat } from "@ai-sdk/react";
import { DefaultChatTransport } from "ai";
import type { UIMessage } from "ai";
import { useMemo, useEffect, useRef, useState } from "react";
import type { AiModelInfo, ChatMessage } from "@interview/shared/types";
import { apiGet, getApiUrl } from "@interview/shared/lib/api-client";
import {
  Conversation,
  ConversationContent,
  ConversationEmptyState,
  ConversationScrollButton,
} from "@interview/shared/components/ai-elements/conversation";
import {
  Message,
  MessageContent,
  MessageResponse,
} from "@interview/shared/components/ai-elements/message";
import {
  PromptInput,
  PromptInputFooter,
  PromptInputSelect,
  PromptInputSelectContent,
  PromptInputSelectItem,
  PromptInputSelectTrigger,
  PromptInputSelectValue,
  PromptInputSubmit,
  PromptInputTextarea,
} from "@interview/shared/components/ai-elements/prompt-input";

interface AiChatPanelProps {
  interviewId: string;
  aiEnabled?: boolean;
}

function getTextContent(message: UIMessage): string {
  return message.parts
    .filter((p): p is Extract<typeof p, { type: "text" }> => p.type === "text")
    .map((p) => p.text)
    .join("");
}

export function AiChatPanel({ interviewId, aiEnabled = true }: AiChatPanelProps) {
  const [models, setModels] = useState<AiModelInfo[]>([]);
  const [selectedModelId, setSelectedModelId] = useState("");
  // 用 ref 存模型 ID，讓 transport body function 每次 request 動態取值，
  // 而不需要重建 transport（重建會清空 messages）
  const modelIdRef = useRef(selectedModelId);
  modelIdRef.current = selectedModelId;

  const transport = useMemo(
    () =>
      new DefaultChatTransport({
        api: getApiUrl(`/interviews/${interviewId}/ai/chat/stream`),
        body: () => ({ modelId: modelIdRef.current }),
      }),
    [interviewId]
  );

  const { messages, setMessages, sendMessage, status, error, stop } = useChat({ transport });

  useEffect(() => {
    apiGet<AiModelInfo[]>("/ai/models")
      .then((list) => {
        setModels(list);
        if (list.length > 0) setSelectedModelId(list[0].id);
      })
      .catch((err) => {
        console.error("[AiChatPanel] Failed to load AI models:", err);
      });
  }, []);

  useEffect(() => {
    async function loadHistory() {
      const res = await fetch(getApiUrl(`/interviews/${interviewId}/ai/history`));
      if (!res.ok) return;
      const data: { messages: ChatMessage[] } = await res.json();
      const history: UIMessage[] = data.messages
        .filter((m) => m.role !== "SYSTEM")
        .map((m) => ({
          id: m.id,
          role: m.role.toLowerCase() as "user" | "assistant",
          parts: [{ type: "text" as const, text: m.content }],
          createdAt: new Date(m.createdAt),
        }));
      if (history.length > 0) {
        setMessages(history);
      }
    }
    loadHistory();
  }, [interviewId, setMessages]);

  return (
    <div className="flex flex-col h-full bg-[#1e1e1e]">
      {!aiEnabled ? (
        <div className="flex flex-1 items-center justify-center p-6 text-center">
          <div>
            <p className="text-sm font-medium text-[#858585] mb-1">此階段 AI 不可用</p>
            <p className="text-xs text-[#585858]">
              本階段考驗您獨立解題能力，請不要依賴 AI 提示
            </p>
          </div>
        </div>
      ) : (
        <>
          <Conversation className="flex-1">
            <ConversationContent className="p-3">
              {messages.length === 0 && (
                <ConversationEmptyState
                  title="準備好了"
                  description="您可以詢問 AI 助理關於題目的提示或說明"
                />
              )}
              {messages.map((message) => (
                <Message key={message.id} from={message.role}>
                  <MessageContent>
                    {message.role === "assistant" ? (
                      <MessageResponse>{getTextContent(message)}</MessageResponse>
                    ) : (
                      <p className="whitespace-pre-wrap text-sm">
                        {getTextContent(message)}
                      </p>
                    )}
                  </MessageContent>
                </Message>
              ))}
            </ConversationContent>
            <ConversationScrollButton />
          </Conversation>

          {error && (
            <p className="text-xs text-red-400 text-center px-3 py-1 shrink-0">
              {error.message}
            </p>
          )}

          <div className="p-3 border-t border-[#333] shrink-0">
            <PromptInput
              onSubmit={(msg) => {
                sendMessage({ text: msg.text ?? "" });
              }}
            >
              <PromptInputTextarea placeholder="輸入問題...（Enter 送出，Shift+Enter 換行）" />
              <PromptInputFooter>
                {models.length > 1 ? (
                  <PromptInputSelect value={selectedModelId} onValueChange={setSelectedModelId}>
                    {/* 深色 pill 風格，在 #1e1e1e 背景上清晰可見 */}
                    <PromptInputSelectTrigger className="h-7 w-auto max-w-[200px] text-xs bg-[#2d2d2d] border border-[#404040] rounded-md text-[#cccccc] hover:bg-[#353535] hover:text-[#e0e0e0]">
                      <PromptInputSelectValue />
                    </PromptInputSelectTrigger>
                    <PromptInputSelectContent>
                      {models.map((m) => (
                        <PromptInputSelectItem key={m.id} value={m.id}>{m.name}</PromptInputSelectItem>
                      ))}
                    </PromptInputSelectContent>
                  </PromptInputSelect>
                ) : models.length === 1 ? (
                  // 單一模型時顯示純文字 pill，讓使用者知道當前模型
                  <span className="text-xs text-[#999] bg-[#2d2d2d] border border-[#404040] rounded-md px-2.5 py-1 select-none">
                    {models[0].name}
                  </span>
                ) : (
                  <div />
                )}
                <PromptInputSubmit status={status} onStop={stop} />
              </PromptInputFooter>
            </PromptInput>
          </div>
        </>
      )}
    </div>
  );
}
