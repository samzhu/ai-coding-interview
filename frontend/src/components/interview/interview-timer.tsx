"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { Clock } from "lucide-react";
import { getApiUrl } from "@/lib/api-client";

interface InterviewTimerProps {
  interviewId: string;
  onExpired?: () => void;
}

function formatTime(seconds: number): string {
  if (seconds <= 0) return "00:00";
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

export function InterviewTimer({ interviewId, onExpired }: InterviewTimerProps) {
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);
  const [totalSeconds, setTotalSeconds] = useState<number>(3600);
  const onExpiredRef = useRef(onExpired);
  onExpiredRef.current = onExpired;
  const hasExpiredRef = useRef(false);

  const syncWithServer = useCallback(async () => {
    try {
      const res = await fetch(getApiUrl(`/interviews/${interviewId}/time-remaining`));
      if (!res.ok) return;
      const data: { remainingSeconds: number; totalSeconds: number } = await res.json();
      setRemainingSeconds(data.remainingSeconds);
      setTotalSeconds(data.totalSeconds);
    } catch {
      // silent fail
    }
  }, [interviewId]);

  // 初次載入同步伺服器時間
  useEffect(() => {
    syncWithServer();
  }, [syncWithServer]);

  // 每 30 秒同步一次伺服器時間
  useEffect(() => {
    const syncInterval = setInterval(syncWithServer, 30_000);
    return () => clearInterval(syncInterval);
  }, [syncWithServer]);

  // 偵測超時（確保只觸發一次）
  useEffect(() => {
    if (remainingSeconds === 0 && !hasExpiredRef.current) {
      hasExpiredRef.current = true;
      onExpiredRef.current?.();
    }
  }, [remainingSeconds]);

  // 本地倒計時（每秒）
  useEffect(() => {
    if (remainingSeconds === null || remainingSeconds <= 0) return;

    const timer = setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev === null || prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [remainingSeconds]);

  if (remainingSeconds === null) {
    return (
      <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
        <Clock className="h-4 w-4" />
        <span>--:--</span>
      </div>
    );
  }

  const isWarning = remainingSeconds <= 300; // 5 分鐘警告
  const isCritical = remainingSeconds <= 60;  // 1 分鐘緊急

  return (
    <div className={`flex items-center gap-1.5 text-sm font-mono ${
      isCritical
        ? "text-destructive font-bold animate-pulse"
        : isWarning
        ? "text-orange-500 font-semibold"
        : "text-muted-foreground"
    }`}>
      <Clock className="h-4 w-4" />
      <span>{formatTime(remainingSeconds)}</span>
      {isWarning && !isCritical && (
        <span className="text-xs text-orange-400">（剩餘不多）</span>
      )}
      {isCritical && (
        <span className="text-xs text-destructive">（即將結束）</span>
      )}
    </div>
  );
}
