"use client";

import { Badge } from "@interview/shared/components/ui/badge";

export interface CheckpointStatus {
  checkpointId: string;
  sequenceNumber: number;
  status: "PENDING" | "PASSED" | "FAILED";
}

interface MonitorCheckpointProgressProps {
  checkpoints: CheckpointStatus[];
}

const STATUS_CONFIG = {
  PENDING: { label: "等待中", className: "bg-muted/50 text-muted-foreground border-0" },
  PASSED:  { label: "通過",   className: "bg-emerald-500/15 text-emerald-400 border-0" },
  FAILED:  { label: "失敗",   className: "bg-red-500/15 text-red-400 border-0" },
};

export function MonitorCheckpointProgress({
  checkpoints,
}: MonitorCheckpointProgressProps) {
  if (checkpoints.length === 0) {
    return (
      <div className="flex items-center justify-center h-full text-sm text-muted-foreground">
        尚無 Checkpoint 資料
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2 p-3">
      {checkpoints.map((cp) => {
        const config = STATUS_CONFIG[cp.status];
        return (
          <div
            key={cp.checkpointId}
            className="flex items-center justify-between p-2 rounded border bg-card"
          >
            <span className="text-sm font-medium">
              Checkpoint {cp.sequenceNumber}
            </span>
            <Badge className={config.className}>{config.label}</Badge>
          </div>
        );
      })}
    </div>
  );
}
