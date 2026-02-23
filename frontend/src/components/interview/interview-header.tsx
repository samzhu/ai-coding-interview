"use client";

import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { useInterview } from "@/contexts/interview-context";

const STATUS_LABELS: Record<string, string> = {
  SCHEDULED: "待開始",
  IN_PROGRESS: "進行中",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
};

const STATUS_VARIANTS: Record<
  string,
  "default" | "secondary" | "destructive" | "outline"
> = {
  SCHEDULED: "secondary",
  IN_PROGRESS: "default",
  COMPLETED: "secondary",
  CANCELLED: "destructive",
};

interface InterviewHeaderProps {
  title: string;
  totalCheckpoints: number;
}

export function InterviewHeader({ title, totalCheckpoints }: InterviewHeaderProps) {
  const { state } = useInterview();
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    if (state.status !== "IN_PROGRESS") return;
    const interval = setInterval(() => setElapsed((e) => e + 1), 1000);
    return () => clearInterval(interval);
  }, [state.status]);

  const minutes = Math.floor(elapsed / 60)
    .toString()
    .padStart(2, "0");
  const seconds = (elapsed % 60).toString().padStart(2, "0");

  const currentSeq = state.checkpoint?.sequenceNumber ?? 1;

  return (
    <header className="flex items-center gap-4 px-4 py-3 border-b bg-background shrink-0">
      <h1 className="font-semibold text-base truncate flex-1">{title}</h1>
      <Badge variant={STATUS_VARIANTS[state.status]}>
        {STATUS_LABELS[state.status]}
      </Badge>
      <span className="text-sm text-muted-foreground">
        Checkpoint {currentSeq}/{totalCheckpoints}
      </span>
      {state.status === "IN_PROGRESS" && (
        <span className="text-sm font-mono tabular-nums text-muted-foreground">
          {minutes}:{seconds}
        </span>
      )}
    </header>
  );
}
