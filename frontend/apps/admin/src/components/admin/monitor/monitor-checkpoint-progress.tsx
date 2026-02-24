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
  PENDING: { label: "等待中", variant: "outline" as const },
  PASSED: { label: "通過", variant: "default" as const },
  FAILED: { label: "失敗", variant: "destructive" as const },
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
            <Badge variant={config.variant}>{config.label}</Badge>
          </div>
        );
      })}
    </div>
  );
}
