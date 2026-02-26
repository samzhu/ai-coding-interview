"use client";

import { useEffect, useState } from "react";
import { InterviewProvider } from "@/contexts/interview-context";
import { InterviewWorkspace } from "@/components/interview/interview-workspace";
import { useRouteParam } from "@interview/shared/hooks/use-route-param";
import { apiGet, apiPost } from "@interview/shared/lib/api-client";
import type {
  InterviewResponse,
  InterviewStatus,
  CheckpointResultResponse,
  WorkspaceFileEntry,
  FileContentResponse,
  CheckpointFileState,
  ContainerStatusResponse,
} from "@interview/shared/types";

async function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Polls /container-status every 2 seconds until READY or FAILED.
 * Throws if the container enters FAILED state.
 */
async function pollContainerStatus(id: string): Promise<void> {
  while (true) {
    const res = await apiGet<ContainerStatusResponse>(
      `/interviews/${id}/container-status`
    );
    if (res.status === "READY") return;
    if (res.status === "FAILED") {
      throw new Error("Container initialization failed");
    }
    await sleep(2000);
  }
}

export function InterviewClient() {
  const id = useRouteParam("/interview/:id", "id");

  const [status, setStatus] = useState<InterviewStatus>("IN_PROGRESS");
  const [checkpoint, setCheckpoint] = useState<CheckpointResultResponse | null>(null);
  // 保留完整 CheckpointResultResponse[]，供 context 的 stepper 做資料驅動狀態判斷
  const [allCheckpoints, setAllCheckpoints] = useState<CheckpointResultResponse[]>([]);
  const [initialFiles, setInitialFiles] = useState<Map<string, CheckpointFileState>>(new Map());
  const [title, setTitle] = useState<string>("面試進行中");
  const [loading, setLoading] = useState(true);
  const [loadingMessage, setLoadingMessage] = useState("載入中...");
  const [notFound, setNotFound] = useState(false);
  const [containerFailed, setContainerFailed] = useState(false);

  useEffect(() => {
    if (!id) return;

    async function init() {
      try {
        // Step 1: Start or get interview
        let interview: InterviewResponse | null = null;
        try {
          interview = await apiPost<InterviewResponse>(`/interviews/${id}/start`);
        } catch {
          try {
            interview = await apiGet<InterviewResponse>(`/interviews/${id}`);
          } catch {
            // ignore
          }
        }

        if (!interview) {
          setNotFound(true);
          return;
        }

        setStatus((interview.status ?? "IN_PROGRESS") as InterviewStatus);
        setTitle(interview.title ?? "面試進行中");

        // Step 2: Wait for container to become READY if it is still initialising.
        // containerStatus is null when no Docker image is configured — skip polling.
        if (interview.containerStatus === "FAILED") {
          setContainerFailed(true);
          return;
        }

        if (interview.containerStatus === "INITIALIZING") {
          setLoadingMessage("正在準備面試環境，請稍候...");
          try {
            await pollContainerStatus(id);
          } catch {
            setContainerFailed(true);
            return;
          }
        }

        // Step 3: Load checkpoints + workspace files (container is READY or not needed)
        setLoadingMessage("載入題目中...");

        let cp: CheckpointResultResponse | null = null;
        try {
          cp = await apiGet<CheckpointResultResponse>(
            `/interviews/${id}/checkpoints/current`
          );
        } catch {
          // No checkpoints configured yet — continue and show workspace
        }

        setCheckpoint(cp);

        // 載入全部 checkpoints 供左側 stepper 顯示
        try {
          const list = await apiGet<CheckpointResultResponse[]>(
            `/interviews/${id}/checkpoints`
          );
          // 直接使用完整資料（含 status），不裁剪欄位，讓 stepper 可以自由跳選
          setAllCheckpoints(list);
        } catch {
          // 無 checkpoints 時不影響進入工作區
        }

        try {
          const entries = await apiGet<WorkspaceFileEntry[]>(
            `/interviews/${id}/files`
          );
          const fileEntries = entries.filter((e) => !e.isDirectory);
          const contents = await Promise.all(
            fileEntries.map((e) =>
              apiGet<FileContentResponse>(
                `/interviews/${id}/files/content?path=${encodeURIComponent(e.filePath)}`
              )
            )
          );
          const fileMap = new Map<string, CheckpointFileState>();
          for (const fc of contents) {
            fileMap.set(fc.filePath, { filePath: fc.filePath, content: fc.content });
          }
          setInitialFiles(fileMap);
        } catch {
          // Container might not be running yet; workspace will show empty
        }
      } finally {
        setLoading(false);
      }
    }

    init();
  }, [id]);

  if (loading) {
    return (
      <div className="flex flex-col h-screen bg-background items-center justify-center gap-3">
        <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
        <p className="text-muted-foreground text-sm">{loadingMessage}</p>
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="flex flex-col h-screen bg-background items-center justify-center">
        <p className="text-muted-foreground">找不到面試</p>
      </div>
    );
  }

  if (containerFailed) {
    return (
      <div className="flex flex-col h-screen bg-background items-center justify-center gap-2">
        <p className="text-destructive font-medium">環境準備失敗</p>
        <p className="text-muted-foreground text-sm">請聯繫面試官重新安排面試</p>
      </div>
    );
  }

  return (
    <InterviewProvider
      interviewId={id}
      initialStatus={status}
      initialCheckpoint={checkpoint}
      initialCheckpoints={allCheckpoints}
      initialFiles={initialFiles}
    >
      <div className="h-screen overflow-hidden">
        <InterviewWorkspace
          interviewId={id}
          title={title}
        />
      </div>
    </InterviewProvider>
  );
}
