"use client";

import { useInterview } from "@/contexts/interview-context";
import { Badge } from "@/components/ui/badge";

export function CheckpointPanel() {
  const { state } = useInterview();
  const { checkpoint } = state;

  if (!checkpoint) {
    return (
      <div className="p-4 text-sm text-muted-foreground">載入題目中...</div>
    );
  }

  const statusColor =
    checkpoint.status === "PASSED"
      ? "default"
      : checkpoint.status === "FAILED"
        ? "destructive"
        : "secondary";

  return (
    <div className="h-full overflow-y-auto p-4 space-y-4">
      <div className="flex items-center gap-2">
        <h2 className="font-semibold text-sm">{checkpoint.title}</h2>
        <Badge variant={statusColor} className="text-xs">
          {checkpoint.status === "PASSED"
            ? "通過"
            : checkpoint.status === "FAILED"
              ? "未通過"
              : "待完成"}
        </Badge>
      </div>

      <div className="text-sm text-foreground/80 whitespace-pre-wrap leading-relaxed">
        {checkpoint.description}
      </div>
    </div>
  );
}
