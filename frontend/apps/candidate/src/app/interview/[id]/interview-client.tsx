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
} from "@interview/shared/types";

export function InterviewClient() {
  const id = useRouteParam("/interview/:id", "id");

  const [status, setStatus] = useState<InterviewStatus>("IN_PROGRESS");
  const [checkpoint, setCheckpoint] = useState<CheckpointResultResponse | null>(null);
  const [initialFiles, setInitialFiles] = useState<Map<string, CheckpointFileState>>(new Map());
  const [title, setTitle] = useState<string>("面試進行中");
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!id) return;

    async function init() {
      try {
        // Start or get interview
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

        // Fetch current checkpoint. May not exist if exam.yml was missing at start time
        // (Task B: interview starts gracefully even without exam.yml, skipping checkpoint init).
        // In that case, keep cp=null and let the workspace render without a checkpoint task.
        let cp: CheckpointResultResponse | null = null;
        try {
          cp = await apiGet<CheckpointResultResponse>(
            `/interviews/${id}/checkpoints/current`
          );
        } catch {
          // No checkpoints configured yet — continue and show workspace
        }

        setCheckpoint(cp);

        // Fetch initial workspace files from container
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
      <div className="flex flex-col h-screen bg-background items-center justify-center">
        <p className="text-muted-foreground">載入中...</p>
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

  return (
    <InterviewProvider
      interviewId={id}
      initialStatus={status}
      initialCheckpoint={checkpoint}
      initialCheckpoints={[]}
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
