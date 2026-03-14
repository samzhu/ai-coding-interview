"use client";

import { useEffect, useState } from "react";
import { Badge } from "@interview/shared/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@interview/shared/components/ui/card";
import { apiGet } from "@interview/shared/lib/api-client";
import type { ChatMessage, ConversationHistoryResponse } from "@interview/shared/types";
import { MessageResponse } from "@interview/shared/components/ai-elements/message";
import { stripEditProposals } from "@interview/shared/lib/edit-proposal-utils";

interface ConversationHistoryProps {
  interviewId: string;
}

/**
 * 設計說明：Admin 面試詳情頁的 AI 對話紀錄元件。
 * 自行 fetch 對話歷史，頂部顯示摘要（訊息數、總 token），
 * 下方逐則顯示 USER/ASSISTANT 訊息，ASSISTANT 訊息附帶模型名稱與 token 統計。
 */
export function ConversationHistory({ interviewId }: ConversationHistoryProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiGet<ConversationHistoryResponse>(`/interviews/${interviewId}/ai/history`)
      .then((data) => setMessages(data.messages ?? []))
      .catch(() => setMessages([]))
      .finally(() => setLoading(false));
  }, [interviewId]);

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>AI 對話紀錄</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">載入中...</p>
        </CardContent>
      </Card>
    );
  }

  // 顯示 USER、ASSISTANT 和 TOOL_CALL（工具執行步驟），過濾 SYSTEM
  const visibleMessages = messages.filter(
    (m) => m.role === "USER" || m.role === "ASSISTANT" || m.role === "TOOL_CALL"
  );

  if (visibleMessages.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>AI 對話紀錄</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">尚無 AI 對話紀錄</p>
        </CardContent>
      </Card>
    );
  }

  // 摘要統計（僅計算 USER/ASSISTANT，排除 TOOL_CALL 條目）
  const assistantMessages = visibleMessages.filter((m) => m.role === "ASSISTANT");
  const totalPromptTokens = assistantMessages.reduce(
    (sum, m) => sum + (m.promptTokens ?? 0),
    0
  );
  const totalCompletionTokens = assistantMessages.reduce(
    (sum, m) => sum + (m.completionTokens ?? 0),
    0
  );
  const totalTokens = totalPromptTokens + totalCompletionTokens;
  const modelCounts = assistantMessages.reduce<Record<string, number>>((acc, m) => {
    const key = m.model ?? "unknown";
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});

  return (
    <Card>
      <CardHeader>
        <CardTitle>AI 對話紀錄</CardTitle>
        <CardDescription className="flex gap-4">
          <span>{visibleMessages.length} 則訊息</span>
          {totalTokens > 0 && (
            <span>
              Token 消耗：{totalPromptTokens.toLocaleString()} prompt + {totalCompletionTokens.toLocaleString()} completion = {totalTokens.toLocaleString()} total
            </span>
          )}
          {Object.keys(modelCounts).length > 0 && (
            <span>
              模型：{Object.entries(modelCounts).map(([model, count]) => `${model} (${count})`).join("、")}
            </span>
          )}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4 max-h-[600px] overflow-y-auto">
          {visibleMessages.map((msg) => {
            // TOOL_CALL：緊湊工具步驟列，視覺上比 USER/ASSISTANT 訊息更小
            if (msg.role === "TOOL_CALL") {
              return (
                <div key={msg.id} className="flex items-center gap-2 pl-4 py-0.5">
                  <span className="text-xs text-emerald-500">✓</span>
                  <span className="text-xs text-muted-foreground">{msg.content}</span>
                  <span className="text-xs text-muted-foreground/50 ml-auto">
                    {new Date(msg.createdAt).toLocaleTimeString("zh-TW")}
                  </span>
                </div>
              );
            }

            return (
              <div key={msg.id} className="flex flex-col gap-1">
                <div className="flex items-center gap-2">
                  <Badge
                    variant={msg.role === "USER" ? "default" : "secondary"}
                    className="text-xs"
                  >
                    {msg.role === "USER" ? "候選人" : "AI"}
                  </Badge>
                  <span className="text-xs text-muted-foreground">
                    {new Date(msg.createdAt).toLocaleString("zh-TW")}
                  </span>
                  {msg.role === "ASSISTANT" && msg.model && (
                    <Badge variant="outline" className="text-xs font-normal">
                      {msg.model}
                    </Badge>
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
                  <div className="text-sm whitespace-pre-wrap bg-muted/30 rounded p-2">
                    {msg.content}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
}
