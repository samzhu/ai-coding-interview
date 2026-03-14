"use client";

import { useEffect, useRef } from "react";
import { Badge } from "@interview/shared/components/ui/badge";
import { MessageResponse } from "@interview/shared/components/ai-elements/message";
import { stripEditProposals } from "@interview/shared/lib/edit-proposal-utils";

export interface ChatMessage {
  role: "USER" | "ASSISTANT" | "TOOL_CALL";
  content: string;
  timestamp: Date;
  model?: string | null;
  promptTokens?: number | null;
  completionTokens?: number | null;
  /** TOOL_CALL 時標示執行狀態（running / completed / error） */
  toolState?: "running" | "completed" | "error";
  /** TOOL_CALL 時的工具呼叫 ID，用於 running → completed 狀態更新去重 */
  toolCallId?: string;
}

interface MonitorChatViewProps {
  messages: ChatMessage[];
}

export function MonitorChatView({ messages }: MonitorChatViewProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  if (messages.length === 0) {
    return (
      <div className="flex items-center justify-center h-full text-sm text-muted-foreground">
        尚無 AI 對話紀錄
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3 p-3 overflow-y-auto h-full">
      {messages.map((msg, i) => {
        // TOOL_CALL 訊息：緊湊的工具執行步驟列
        if (msg.role === "TOOL_CALL") {
          const stateIcon =
            msg.toolState === "running"
              ? "⟳"
              : msg.toolState === "error"
              ? "✗"
              : "✓";
          const stateColor =
            msg.toolState === "running"
              ? "text-amber-400"
              : msg.toolState === "error"
              ? "text-red-400"
              : "text-emerald-400";
          return (
            <div key={i} className="flex items-center gap-1.5 pl-2 py-0.5">
              <span className={`text-xs font-mono ${stateColor}`}>{stateIcon}</span>
              <span className="text-xs text-muted-foreground truncate">{msg.content}</span>
              <span className="text-xs text-muted-foreground/50 ml-auto shrink-0">
                {msg.timestamp.toLocaleTimeString("zh-TW")}
              </span>
            </div>
          );
        }

        // USER / ASSISTANT 訊息
        return (
          <div key={i} className="flex flex-col gap-1">
            <div className="flex items-center gap-2">
              <Badge
                variant={msg.role === "USER" ? "default" : "secondary"}
                className="text-xs"
              >
                {msg.role === "USER" ? "候選人" : "AI"}
              </Badge>
              <span className="text-xs text-muted-foreground">
                {msg.timestamp.toLocaleTimeString("zh-TW")}
              </span>
              {msg.role === "ASSISTANT" && msg.model && (
                <span className="text-xs text-muted-foreground">
                  {msg.model}
                </span>
              )}
              {msg.role === "ASSISTANT" &&
                msg.promptTokens != null &&
                msg.completionTokens != null && (
                  <span className="text-xs text-muted-foreground">
                    {msg.promptTokens} + {msg.completionTokens} ={" "}
                    {msg.promptTokens + msg.completionTokens} tokens
                  </span>
                )}
            </div>
            {msg.role === "ASSISTANT" ? (
              <div className="text-sm bg-muted/30 rounded p-2">
                <MessageResponse>
                  {stripEditProposals(msg.content)}
                </MessageResponse>
              </div>
            ) : (
              <p className="text-sm whitespace-pre-wrap bg-muted/30 rounded p-2">
                {msg.content}
              </p>
            )}
          </div>
        );
      })}
      <div ref={bottomRef} />
    </div>
  );
}
