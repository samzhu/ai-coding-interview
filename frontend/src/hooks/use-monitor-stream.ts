"use client";

import { useEffect, useRef } from "react";
import { getApiUrl } from "@/lib/api-client";

export interface CodeChangeEvent {
  type: "code-change";
  filePath: string;
  code: string;
}

export interface CheckpointEvent {
  type: "checkpoint-passed" | "checkpoint-failed";
  checkpointId: string;
  sequenceNumber: number;
  passed: boolean;
}

export interface AiMessageEvent {
  type: "ai-prompt-sent" | "ai-response-received";
  role: string;
  content: string;
}

export interface InterviewCompletedEvent {
  type: "interview-completed";
  interviewId: string;
}

export type MonitorEvent =
  | CodeChangeEvent
  | CheckpointEvent
  | AiMessageEvent
  | InterviewCompletedEvent;

export interface MonitorStreamHandlers {
  onCodeChange?: (event: CodeChangeEvent) => void;
  onCheckpointPassed?: (event: CheckpointEvent) => void;
  onCheckpointFailed?: (event: CheckpointEvent) => void;
  onAiPromptSent?: (event: AiMessageEvent) => void;
  onAiResponseReceived?: (event: AiMessageEvent) => void;
  onInterviewCompleted?: (event: InterviewCompletedEvent) => void;
}

export function useMonitorStream(
  interviewId: string,
  handlers: MonitorStreamHandlers
) {
  const handlersRef = useRef(handlers);
  handlersRef.current = handlers;

  useEffect(() => {
    const es = new EventSource(
      getApiUrl(`/interviews/${interviewId}/monitor/stream`)
    );

    es.addEventListener("code-change", (e: MessageEvent) => {
      try {
        const data: CodeChangeEvent = JSON.parse(e.data);
        handlersRef.current.onCodeChange?.(data);
      } catch {
        // ignore malformed events
      }
    });

    es.addEventListener("checkpoint-passed", (e: MessageEvent) => {
      try {
        const data: CheckpointEvent = JSON.parse(e.data);
        handlersRef.current.onCheckpointPassed?.(data);
      } catch {
        // ignore malformed events
      }
    });

    es.addEventListener("checkpoint-failed", (e: MessageEvent) => {
      try {
        const data: CheckpointEvent = JSON.parse(e.data);
        handlersRef.current.onCheckpointFailed?.(data);
      } catch {
        // ignore malformed events
      }
    });

    es.addEventListener("ai-prompt-sent", (e: MessageEvent) => {
      try {
        const data: AiMessageEvent = JSON.parse(e.data);
        handlersRef.current.onAiPromptSent?.(data);
      } catch {
        // ignore malformed events
      }
    });

    es.addEventListener("ai-response-received", (e: MessageEvent) => {
      try {
        const data: AiMessageEvent = JSON.parse(e.data);
        handlersRef.current.onAiResponseReceived?.(data);
      } catch {
        // ignore malformed events
      }
    });

    es.addEventListener("interview-completed", (e: MessageEvent) => {
      try {
        const data: InterviewCompletedEvent = JSON.parse(e.data);
        handlersRef.current.onInterviewCompleted?.(data);
      } catch {
        // ignore malformed events
      }
    });

    return () => {
      es.close();
    };
  }, [interviewId]);
}
