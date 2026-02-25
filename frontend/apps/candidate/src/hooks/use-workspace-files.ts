"use client";

import { useCallback, useRef } from "react";
import { apiGet, apiPut } from "@interview/shared/lib/api-client";
import { useInterview } from "@/contexts/interview-context";
import type {
  WorkspaceFileEntry,
  FileContentResponse,
  CheckpointFileState,
} from "@interview/shared/types";

export function useWorkspaceFiles(interviewId: string) {
  const { loadFiles } = useInterview();
  const debounceTimers = useRef<Map<string, ReturnType<typeof setTimeout>>>(
    new Map()
  );

  const saveFileNow = useCallback(
    async (filePath: string, content: string) => {
      await apiPut<void>(
        `/interviews/${interviewId}/files/content?path=${encodeURIComponent(filePath)}`,
        { content }
      );
    },
    [interviewId]
  );

  const saveFileDebounced = useCallback(
    (filePath: string, content: string) => {
      const existing = debounceTimers.current.get(filePath);
      if (existing) clearTimeout(existing);

      const timer = setTimeout(async () => {
        debounceTimers.current.delete(filePath);
        await saveFileNow(filePath, content).catch(() => {
          // silent fail — best effort write
        });
      }, 1000);

      debounceTimers.current.set(filePath, timer);
    },
    [saveFileNow]
  );

  const loadWorkspaceFiles = useCallback(async () => {
    try {
      const entries = await apiGet<WorkspaceFileEntry[]>(
        `/interviews/${interviewId}/files`
      );
      const fileEntries = entries.filter((e) => !e.isDirectory);
      const contents = await Promise.all(
        fileEntries.map((e) =>
          apiGet<FileContentResponse>(
            `/interviews/${interviewId}/files/content?path=${encodeURIComponent(e.filePath)}`
          )
        )
      );
      const fileMap = new Map<string, CheckpointFileState>();
      for (const fc of contents) {
        fileMap.set(fc.filePath, { filePath: fc.filePath, content: fc.content });
      }
      loadFiles(fileMap);
    } catch {
      // Container may not be ready yet; caller handles this
    }
  }, [interviewId, loadFiles]);

  return { loadWorkspaceFiles, saveFileDebounced, saveFileNow };
}
