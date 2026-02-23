"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { InterviewListTable } from "@/components/admin/interview-list-table";
import { apiGet } from "@/lib/api-client";
import type { InterviewResponse } from "@/types/interview";

export default function InterviewsPage() {
  const [interviews, setInterviews] = useState<InterviewResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiGet<InterviewResponse[]>("/interviews")
      .then(setInterviews)
      .catch(() => setInterviews([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold">面試列表</h1>
            <p className="text-muted-foreground mt-1">
              {loading ? "載入中..." : `共 ${interviews.length} 筆記錄`}
            </p>
          </div>
          <div className="flex gap-3">
            <Button variant="outline" asChild>
              <Link href="/admin">返回後台</Link>
            </Button>
            <Button asChild>
              <Link href="/admin/interviews/new">建立新面試</Link>
            </Button>
          </div>
        </div>

        {loading ? (
          <div className="text-center py-12 text-muted-foreground">載入中...</div>
        ) : (
          <InterviewListTable interviews={interviews} />
        )}
      </div>
    </div>
  );
}
