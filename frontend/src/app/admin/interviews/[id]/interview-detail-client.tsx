"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useRouteParam } from "@/hooks/use-route-param";
import { apiGet } from "@/lib/api-client";
import type { InterviewResponse } from "@/types";

const STATUS_LABELS: Record<string, string> = {
  SCHEDULED: "已排定",
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
  COMPLETED: "outline",
  CANCELLED: "destructive",
};

function formatDate(dateStr: string | null): string {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleString("zh-TW");
}

export function InterviewDetailClient() {
  const id = useRouteParam("/admin/interviews/:id", "id");
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
      <div className="min-h-screen bg-background flex items-center justify-center">
        <p className="text-muted-foreground">載入中...</p>
      </div>
    );
  }

  if (notFound || !interview) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <p className="text-muted-foreground">找不到面試</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-4xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold">{interview.title}</h1>
          <Button variant="outline" asChild>
            <Link href="/admin/interviews">返回面試列表</Link>
          </Button>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              面試詳情
              <Badge variant={STATUS_VARIANTS[interview.status] ?? "secondary"}>
                {STATUS_LABELS[interview.status] ?? interview.status}
              </Badge>
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
              <p className="text-muted-foreground">AI 模型</p>
              <p className="font-medium">{interview.aiModel ?? "預設"}</p>
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

        <div className="flex gap-2">
          <Button variant="outline" asChild>
            <Link href={`/admin/interviews/${id}/monitor`}>監控面試</Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
