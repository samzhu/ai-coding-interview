"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Badge } from "@interview/shared/components/ui/badge";
import { Button } from "@interview/shared/components/ui/button";
import { apiPost } from "@interview/shared/lib/api-client";
import type { InterviewResponse, InterviewStatus } from "@interview/shared/types";

interface InterviewListTableProps {
  interviews: InterviewResponse[];
}

const STATUS_BADGE: Record<
  InterviewStatus,
  { label: string; variant: "default" | "secondary" | "destructive" | "outline" }
> = {
  SCHEDULED: { label: "已排程", variant: "default" },
  IN_PROGRESS: { label: "進行中", variant: "secondary" },
  COMPLETED: { label: "已完成", variant: "outline" },
  CANCELLED: { label: "已取消", variant: "destructive" },
};

function formatDate(iso: string) {
  return new Date(iso).toLocaleString("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function InterviewListTable({ interviews }: InterviewListTableProps) {
  const router = useRouter();
  const [copyingId, setCopyingId] = useState<string | null>(null);

  async function handleCopyInvite(interviewId: string) {
    setCopyingId(interviewId);
    try {
      const invitation = await apiPost<{ token: string }>(
        `/interviews/${interviewId}/invitation`
      );
      const candidateUrl = process.env.NEXT_PUBLIC_CANDIDATE_URL ?? window.location.origin;
      await navigator.clipboard.writeText(`${candidateUrl}/invite/${invitation.token}`);
    } catch {
      // silent fail — user can retry
    } finally {
      setCopyingId(null);
    }
  }

  async function handleCancel(interviewId: string) {
    if (!window.confirm("確認取消此面試？此操作無法復原。")) return;
    await apiPost(`/interviews/${interviewId}/cancel`);
    router.refresh();
  }

  if (interviews.length === 0) {
    return (
      <p className="text-center text-muted-foreground py-12">
        目前沒有面試記錄，請先建立面試。
      </p>
    );
  }

  return (
    <>
      <div className="rounded-lg border overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-muted/50">
            <tr>
              <th className="text-left px-4 py-3 font-medium">標題</th>
              <th className="text-left px-4 py-3 font-medium">狀態</th>
              <th className="text-left px-4 py-3 font-medium">AI 模型</th>
              <th className="text-left px-4 py-3 font-medium">建立時間</th>
              <th className="text-right px-4 py-3 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            {interviews.map((interview) => {
              const badge = STATUS_BADGE[interview.status];
              return (
                <tr key={interview.id} className="border-t hover:bg-muted/30">
                  <td className="px-4 py-3 font-medium">{interview.title}</td>
                  <td className="px-4 py-3">
                    <Badge variant={badge.variant}>{badge.label}</Badge>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground text-xs">
                    {interview.aiModel ?? "—"}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {formatDate(interview.createdAt)}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-2">
                      <Button variant="outline" size="sm" asChild>
                        <Link href={`/interviews/${interview.id}`}>
                          查看
                        </Link>
                      </Button>
                      {interview.status === "IN_PROGRESS" && (
                        <Button variant="secondary" size="sm" asChild>
                          <Link href={`/interviews/${interview.id}/monitor`}>
                            監控
                          </Link>
                        </Button>
                      )}
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleCopyInvite(interview.id)}
                        disabled={copyingId === interview.id}
                      >
                        {copyingId === interview.id ? "複製中..." : "複製邀請"}
                      </Button>
                      {interview.status !== "CANCELLED" &&
                        interview.status !== "COMPLETED" && (
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => handleCancel(interview.id)}
                          >
                            取消
                          </Button>
                        )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </>
  );
}
