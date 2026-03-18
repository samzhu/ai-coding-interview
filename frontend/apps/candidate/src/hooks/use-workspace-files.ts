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
  const { loadFiles, setFileContent } = useInterview();
  const debounceTimers = useRef<Map<string, ReturnType<typeof setTimeout>>>(
    new Map()
  );
  // Tracks content for pending (debounced) writes not yet flushed to Docker
  const pendingWrites = useRef<Map<string, string>>(new Map());
  // Paths temporarily suppressed from debounced saves (during refreshFiles)
  const suppressedPaths = useRef<Set<string>>(new Set());

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
      // Skip if this path is suppressed (refreshFiles in progress)
      if (suppressedPaths.current.has(filePath)) return;

      pendingWrites.current.set(filePath, content);
      const existing = debounceTimers.current.get(filePath);
      if (existing) clearTimeout(existing);

      const timer = setTimeout(async () => {
        debounceTimers.current.delete(filePath);
        pendingWrites.current.delete(filePath);
        await saveFileNow(filePath, content).catch(() => {
          // silent fail — best effort write
        });
      }, 1000);

      debounceTimers.current.set(filePath, timer);
    },
    [saveFileNow]
  );

  /**
   * 取消所有 pending debounced 計時器，立即將所有 pending 內容寫入 Docker。
   * 設計說明：Run/Submit 前呼叫，確保 Docker 擁有最新的編輯器內容，
   * 解決「用戶打字 → debounce 1s → 立刻按 Run → Docker 跑舊內容」的競態問題。
   */
  const flushPendingSaves = useCallback(async () => {
    const writes: Array<Promise<void>> = [];
    for (const [filePath, content] of pendingWrites.current) {
      const timer = debounceTimers.current.get(filePath);
      if (timer) {
        clearTimeout(timer);
        debounceTimers.current.delete(filePath);
      }
      writes.push(saveFileNow(filePath, content).catch(() => {}));
    }
    pendingWrites.current.clear();
    await Promise.all(writes);
  }, [saveFileNow]);

  // Inner function shared by loadWorkspaceFiles and refreshFiles (full reload)
  const loadWorkspaceFilesInner = useCallback(async () => {
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
  }, [interviewId, loadFiles]);

  const loadWorkspaceFiles = useCallback(async () => {
    try {
      await loadWorkspaceFilesInner();
    } catch {
      // Container may not be ready yet; caller handles this
    }
  }, [loadWorkspaceFilesInner]);

  /**
   * 從 Docker 回讀指定路徑的最新內容，更新編輯器 state。
   * 設計說明：後端 `data-file-changed` SSE 事件觸發此函式，確保編輯器與 Docker 檔案同步。
   * - paths 不為空：逐一 GET 並呼叫 setFileContent 更新指定檔案
   * - paths 為空（[]）：呼叫 loadWorkspaceFiles 全量重新載入
   * 期間抑制這些路徑的 debounced save，防止過期 pending 內容蓋回 Docker。
   */
  const refreshFiles = useCallback(
    async (paths: string[]) => {
      if (paths.length === 0) {
        await loadWorkspaceFiles();
        return;
      }
      // Cancel pending writes and suppress debounced saves for these paths
      for (const p of paths) {
        const timer = debounceTimers.current.get(p);
        if (timer) {
          clearTimeout(timer);
          debounceTimers.current.delete(p);
        }
        pendingWrites.current.delete(p);
        suppressedPaths.current.add(p);
      }
      try {
        const results = await Promise.allSettled(
          paths.map((p) =>
            apiGet<FileContentResponse>(
              `/interviews/${interviewId}/files/content?path=${encodeURIComponent(p)}`
            )
          )
        );
        for (const result of results) {
          if (result.status === "fulfilled") {
            const { filePath, content } = result.value;
            setFileContent(filePath, content);
          }
        }
      } finally {
        for (const p of paths) suppressedPaths.current.delete(p);
      }
    },
    [interviewId, setFileContent, loadWorkspaceFiles]
  );

  return {
    loadWorkspaceFiles,
    saveFileDebounced,
    saveFileNow,
    flushPendingSaves,
    refreshFiles,
  };
}
