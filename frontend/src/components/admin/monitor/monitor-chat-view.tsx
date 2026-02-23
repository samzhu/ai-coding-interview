"use client";

import { useEffect, useRef } from "react";
import { Badge } from "@/components/ui/badge";

export interface ChatMessage {
  role: "USER" | "ASSISTANT";
  content: string;
  timestamp: Date;
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
      {messages.map((msg, i) => (
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
          </div>
          <p className="text-sm whitespace-pre-wrap bg-muted/30 rounded p-2">
            {msg.content}
          </p>
        </div>
      ))}
      <div ref={bottomRef} />
    </div>
  );
}
