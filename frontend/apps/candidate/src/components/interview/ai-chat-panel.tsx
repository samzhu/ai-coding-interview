"use client";

import { useChat } from "@ai-sdk/react";
import { DefaultChatTransport } from "ai";
import type { UIMessage } from "ai";
import { useMemo, useEffect } from "react";
import type { ChatMessage } from "@interview/shared/types";
import { getApiUrl } from "@interview/shared/lib/api-client";
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
  const transport = useMemo(
    () =>
      new DefaultChatTransport({
        api: getApiUrl(`/interviews/${interviewId}/ai/chat/stream`),
      }),
    [interviewId]
  );

  const { messages, setMessages, sendMessage, status, error, stop } = useChat({ transport });

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
    <div className="flex flex-col h-full">
      <div className="px-3 py-2 border-b shrink-0">
        <h3 className="text-sm font-semibold">AI 面試助理</h3>
        <p className="text-xs text-muted-foreground">
          {aiEnabled ? "可詢問提示或說明，不會直接給出答案" : "此階段 AI 停用"}
        </p>
      </div>

      {!aiEnabled ? (
        <div className="flex flex-1 items-center justify-center p-6 text-center">
          <div>
            <p className="text-sm font-medium text-muted-foreground mb-1">此階段 AI 不可用</p>
            <p className="text-xs text-muted-foreground">
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
            <p className="text-xs text-destructive text-center px-3 py-1 shrink-0">
              {error.message}
            </p>
          )}

          <div className="p-3 border-t shrink-0">
            <PromptInput
              onSubmit={(msg) => {
                sendMessage({ text: msg.text ?? "" });
              }}
            >
              <PromptInputTextarea placeholder="輸入問題...（Enter 送出，Shift+Enter 換行）" />
              <PromptInputFooter>
                <div />
                <PromptInputSubmit status={status} onStop={stop} />
              </PromptInputFooter>
            </PromptInput>
          </div>
        </>
      )}
    </div>
  );
}
