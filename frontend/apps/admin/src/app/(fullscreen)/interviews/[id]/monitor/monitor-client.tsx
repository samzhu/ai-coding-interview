"use client";

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { Button } from "@interview/shared/components/ui/button";
import { Badge } from "@interview/shared/components/ui/badge";
import { MonitorCodeView } from "@/components/admin/monitor/monitor-code-view";
import { MonitorChatView, ChatMessage } from "@/components/admin/monitor/monitor-chat-view";
import {
  MonitorCheckpointProgress,
  CheckpointStatus,
} from "@/components/admin/monitor/monitor-checkpoint-progress";
import { useMonitorStream } from "@/hooks/use-monitor-stream";
import { useRouteParam } from "@interview/shared/hooks/use-route-param";
import { apiGet } from "@interview/shared/lib/api-client";
import type { InterviewResponse, CheckpointResultResponse } from "@interview/shared/types";
import type { ConversationHistoryResponse } from "@interview/shared/types";

function detectLanguage(filePath?: string): string {
  if (!filePath) return "python";
  if (filePath.endsWith(".py")) return "python";
  if (filePath.endsWith(".js") || filePath.endsWith(".ts")) return "javascript";
  if (filePath.endsWith(".java")) return "java";
  return "python";
}

export function MonitorClient() {
  const interviewId = useRouteParam("/interviews/:id/monitor", "id");

  const [interview, setInterview] = useState<InterviewResponse | null>(null);
  const [currentCode, setCurrentCode] = useState("");
  const [currentFilePath, setCurrentFilePath] = useState<string | undefined>();
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [checkpoints, setCheckpoints] = useState<CheckpointStatus[]>([]);
  const [isCompleted, setIsCompleted] = useState(false);

  // Load initial data
  useEffect(() => {
    async function loadInitialData() {
      try {
        const [interviewData, checkpointData, historyData] = await Promise.all([
          apiGet<InterviewResponse>(`/interviews/${interviewId}`),
          apiGet<CheckpointResultResponse>(
            `/interviews/${interviewId}/checkpoints/current`
          ).catch(() => null),
          apiGet<ConversationHistoryResponse>(
            `/interviews/${interviewId}/ai/history`
          ).catch(() => ({ messages: [] })),
        ]);

        setInterview(interviewData);

        if (checkpointData) {
          setCurrentCode(checkpointData.submittedCode || checkpointData.starterCode || "");
        }

        if (historyData?.messages) {
          const mapped: ChatMessage[] = historyData.messages
            .filter((m) => m.role !== "SYSTEM")
            .map((m) => ({
              role: m.role as "USER" | "ASSISTANT",
              content: m.content,
              timestamp: new Date(m.createdAt),
            }));
          setChatMessages(mapped);
        }
      } catch {
        // ignore load errors — SSE will bring updates
      }
    }

    loadInitialData();
  }, [interviewId]);

  const handleCodeChange = useCallback(
    (event: { filePath: string; code: string }) => {
      setCurrentCode(event.code);
      if (event.filePath) setCurrentFilePath(event.filePath);
    },
    []
  );

  const handleCheckpointPassed = useCallback(
    (event: { checkpointId: string; sequenceNumber: number }) => {
      setCheckpoints((prev) => {
        const existing = prev.find((c) => c.checkpointId === event.checkpointId);
        if (existing) {
          return prev.map((c) =>
            c.checkpointId === event.checkpointId ? { ...c, status: "PASSED" } : c
          );
        }
        return [
          ...prev,
          {
            checkpointId: event.checkpointId,
            sequenceNumber: event.sequenceNumber,
            status: "PASSED",
          },
        ];
      });
    },
    []
  );

  const handleCheckpointFailed = useCallback(
    (event: { checkpointId: string; sequenceNumber: number }) => {
      setCheckpoints((prev) => {
        const existing = prev.find((c) => c.checkpointId === event.checkpointId);
        if (existing) {
          return prev.map((c) =>
            c.checkpointId === event.checkpointId ? { ...c, status: "FAILED" } : c
          );
        }
        return [
          ...prev,
          {
            checkpointId: event.checkpointId,
            sequenceNumber: event.sequenceNumber,
            status: "FAILED",
          },
        ];
      });
    },
    []
  );

  const handleAiPromptSent = useCallback((event: { content: string }) => {
    setChatMessages((prev) => [
      ...prev,
      { role: "USER", content: event.content, timestamp: new Date() },
    ]);
  }, []);

  const handleAiResponseReceived = useCallback((event: { content: string }) => {
    setChatMessages((prev) => [
      ...prev,
      { role: "ASSISTANT", content: event.content, timestamp: new Date() },
    ]);
  }, []);

  const handleInterviewCompleted = useCallback(() => {
    setIsCompleted(true);
    setInterview((prev) => (prev ? { ...prev, status: "COMPLETED" } : prev));
  }, []);

  useMonitorStream(interviewId, {
    onCodeChange: handleCodeChange,
    onCheckpointPassed: handleCheckpointPassed,
    onCheckpointFailed: handleCheckpointFailed,
    onAiPromptSent: handleAiPromptSent,
    onAiResponseReceived: handleAiResponseReceived,
    onInterviewCompleted: handleInterviewCompleted,
  });

  const language = detectLanguage(currentFilePath);

  return (
    <div className="h-screen flex flex-col bg-background overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-2 border-b shrink-0">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" asChild>
            <Link href={`/interviews/${interviewId}`}>← 返回詳情</Link>
          </Button>
          <h1 className="font-semibold text-sm">
            {interview?.title ?? "面試監控"}
          </h1>
          {interview && (
            <span
              className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                interview.status === "IN_PROGRESS"
                  ? "bg-amber-500/15 text-amber-400"
                  : interview.status === "COMPLETED"
                  ? "bg-emerald-500/15 text-emerald-400"
                  : "bg-blue-500/15 text-blue-400"
              }`}
            >
              {interview.status === "IN_PROGRESS"
                ? "進行中"
                : interview.status === "COMPLETED"
                ? "已完成"
                : interview.status}
            </span>
          )}
          {isCompleted && (
            <Badge variant="outline" className="text-emerald-400 border-emerald-400/30">
              面試已結束
            </Badge>
          )}
        </div>
        <span className="text-xs text-muted-foreground">即時監控 (SSE)</span>
      </div>

      {/* Main Content */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left: Checkpoint Progress (20%) */}
        <div className="w-1/5 border-r flex flex-col overflow-hidden">
          <div className="px-3 py-2 border-b bg-muted/30">
            <h2 className="text-xs font-semibold uppercase text-muted-foreground">
              Checkpoint 進度
            </h2>
          </div>
          <div className="flex-1 overflow-y-auto">
            <MonitorCheckpointProgress checkpoints={checkpoints} />
          </div>
        </div>

        {/* Center: Code View (50%) */}
        <div className="flex-1 flex flex-col overflow-hidden border-r">
          <div className="px-3 py-2 border-b bg-muted/30 shrink-0">
            <h2 className="text-xs font-semibold uppercase text-muted-foreground">
              候選人程式碼（即時同步）
            </h2>
          </div>
          <div className="flex-1 overflow-hidden">
            {currentCode ? (
              <MonitorCodeView
                code={currentCode}
                filePath={currentFilePath}
                language={language}
              />
            ) : (
              <div className="flex items-center justify-center h-full text-sm text-muted-foreground">
                等待候選人開始編寫程式碼...
              </div>
            )}
          </div>
        </div>

        {/* Right: AI Chat Log (30%) */}
        <div className="w-[30%] flex flex-col overflow-hidden">
          <div className="px-3 py-2 border-b bg-muted/30 shrink-0">
            <h2 className="text-xs font-semibold uppercase text-muted-foreground">
              AI 對話紀錄
            </h2>
          </div>
          <div className="flex-1 overflow-hidden">
            <MonitorChatView messages={chatMessages} />
          </div>
        </div>
      </div>
    </div>
  );
}
