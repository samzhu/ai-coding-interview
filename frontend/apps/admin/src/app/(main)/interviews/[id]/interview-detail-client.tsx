"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@interview/shared/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@interview/shared/components/ui/card";
import { useRouteParam } from "@interview/shared/hooks/use-route-param";
import { apiGet } from "@interview/shared/lib/api-client";
import type { InterviewResponse } from "@interview/shared/types";
import { ConversationHistory } from "@/components/admin/conversation-history";

const STATUS_LABELS: Record<string, string> = {
  SCHEDULED: "已排定",
  IN_PROGRESS: "進行中",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
};

const STATUS_CLASS: Record<string, string> = {
  SCHEDULED: "bg-blue-500/15 text-blue-400 border-0",
  IN_PROGRESS: "bg-amber-500/15 text-amber-400 border-0",
  COMPLETED: "bg-emerald-500/15 text-emerald-400 border-0",
  CANCELLED: "bg-red-500/15 text-red-400 border-0",
};

function formatDate(dateStr: string | null): string {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleString("zh-TW");
}

export function InterviewDetailClient() {
  const id = useRouteParam("/interviews/:id", "id");
  const [interview, setInterview] = useState<InterviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!id) return;
    apiGet<InterviewResponse>(`/interviews/${id}`)
      .then(setInterview)
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <p className="text-muted-foreground">載入中...</p>
      </div>
    );
  }

  if (notFound || !interview) {
    return (
      <div className="flex items-center justify-center py-24">
        <p className="text-muted-foreground">找不到面試</p>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold">{interview.title}</h1>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            面試詳情
            <span
              className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_CLASS[interview.status] ?? "bg-muted text-muted-foreground"}`}
            >
              {STATUS_LABELS[interview.status] ?? interview.status}
            </span>
          </CardTitle>
          <CardDescription>面試 ID：{interview.id}</CardDescription>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <p className="text-muted-foreground">應試者 ID</p>
            <p className="font-medium">{interview.candidateId}</p>
          </div>
          <div>
            <p className="text-muted-foreground">面試官 ID</p>
            <p className="font-medium">{interview.interviewerId}</p>
          </div>
          <div>
            <p className="text-muted-foreground">時長（分鐘）</p>
            <p className="font-medium">{interview.durationMinutes}</p>
          </div>
          <div>
            <p className="text-muted-foreground">排定時間</p>
            <p className="font-medium">{formatDate(interview.scheduledAt)}</p>
          </div>
          <div>
            <p className="text-muted-foreground">建立時間</p>
            <p className="font-medium">{formatDate(interview.createdAt)}</p>
          </div>
          <div>
            <p className="text-muted-foreground">開始時間</p>
            <p className="font-medium">{formatDate(interview.startedAt)}</p>
          </div>
          <div>
            <p className="text-muted-foreground">完成時間</p>
            <p className="font-medium">{formatDate(interview.completedAt)}</p>
          </div>
        </CardContent>
      </Card>

      <ConversationHistory interviewId={id!} />

      <div className="flex gap-2">
        <Button variant="outline" asChild>
          <Link href={`/interviews/${id}/monitor`}>監控面試</Link>
        </Button>
      </div>
    </div>
  );
}
