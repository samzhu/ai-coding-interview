"use client";

import { useCallback, useRef, useEffect } from "react";
import { apiPost, apiGet } from "@interview/shared/lib/api-client";
import { useInterview } from "@/contexts/interview-context";
import type { CheckpointResultResponse, ExecutionStatusResponse } from "@interview/shared/types";

const POLL_INTERVAL_MS = 2000;

export function useCodeSubmission() {
  const { state, setRunning, setLiveOutput, switchCheckpoint, applyCheckpointResult } =
    useInterview();
  const pollingRef = useRef(false);

  // 元件卸載時停止 polling
  useEffect(() => {
    return () => {
      pollingRef.current = false;
    };
  }, []);

  const pollForResult = useCallback(
    async (interviewId: string, checkpointId: string, processId: string) => {
      pollingRef.current = true;
      while (pollingRef.current) {
        await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS));
        if (!pollingRef.current) break;
        try {
          const exec = await apiGet<ExecutionStatusResponse>(
            `/interviews/${interviewId}/executions/${processId}`
          );

          // 每次 polling 都更新 live console output（前端直接替換顯示）
          setLiveOutput(exec.consoleOutput);

          if (exec.status !== "RUNNING") {
            pollingRef.current = false;

            // 刷新 checkpoint 狀態（stepper 用）
            const checkpoints = await apiGet<CheckpointResultResponse[]>(
              `/interviews/${interviewId}/checkpoints`
            );
            const cp = checkpoints.find((c) => c.checkpointId === checkpointId);
            if (cp) {
              const execution = {
                exitCode: exec.exitCode ?? (exec.passed ? 0 : 1),
                stdout: exec.consoleOutput ?? "",
                stderr: "",
                durationMs: 0,
                status: (exec.passed ? "SUCCESS" : "RUNTIME_ERROR") as
                  | "SUCCESS"
                  | "RUNTIME_ERROR",
              };
              applyCheckpointResult(cp, execution);

              if (exec.passed) {
                await new Promise((r) => setTimeout(r, 1000));
                const nextCp = [...state.checkpoints]
                  .sort((a, b) => a.sequenceNumber - b.sequenceNumber)
                  .find(
                    (c) =>
                      c.checkpointId !== checkpointId && c.status !== "PASSED"
                  );
                if (nextCp) switchCheckpoint(nextCp.checkpointId);
              }
            } else {
              // checkpoint not found — fallback: stop running
              setRunning(false);
            }
            return;
          }
        } catch {
          pollingRef.current = false;
          setRunning(false);
          return;
        }
      }
    },
    [state.checkpoints, setRunning, setLiveOutput, switchCheckpoint, applyCheckpointResult]
  );

  const submit = useCallback(async () => {
    if (!state.checkpoint || state.isRunning) return;

    setRunning(true);

    try {
      // 檔案已透過 debounced write 寫入 container，直接觸發評分
      const result = await apiPost<CheckpointResultResponse>(
        `/interviews/${state.interviewId}/checkpoints/${state.checkpoint.checkpointId}/submit`
      );

      if (result.status === "IN_PROGRESS" && result.processId) {
        // 非同步模式：後端返回 202，前端透過 processId polling 取得即時輸出與最終結果
        pollForResult(state.interviewId!, result.checkpointId, result.processId);
      } else if (result.status === "IN_PROGRESS") {
        // 舊模式相容：無 processId，回退到整批 polling
        legacyPollForResult(state.interviewId!, result.checkpointId);
      } else {
        // 同步完成（相容性保留）
        const execution = {
          exitCode: result.status === "PASSED" ? 0 : 1,
          stdout: result.executionOutput ?? "",
          stderr: "",
          durationMs: 0,
          status: (result.status === "PASSED" ? "SUCCESS" : "RUNTIME_ERROR") as
            | "SUCCESS"
            | "RUNTIME_ERROR",
        };
        applyCheckpointResult(result, execution);

        if (result.status === "PASSED") {
          await new Promise((r) => setTimeout(r, 1000));
          const nextCp = [...state.checkpoints]
            .sort((a, b) => a.sequenceNumber - b.sequenceNumber)
            .find(
              (c) =>
                c.checkpointId !== result.checkpointId && c.status !== "PASSED"
            );
          if (nextCp) switchCheckpoint(nextCp.checkpointId);
        }
      }
    } catch (err) {
      setRunning(false);
      throw err;
    }
  }, [state, setRunning, switchCheckpoint, applyCheckpointResult, pollForResult]); // eslint-disable-line react-hooks/exhaustive-deps

  /** Legacy fallback: poll GET /checkpoints until status changes */
  const legacyPollForResult = useCallback(
    async (interviewId: string, checkpointId: string) => {
      pollingRef.current = true;
      while (pollingRef.current) {
        await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS));
        if (!pollingRef.current) break;
        try {
          const checkpoints = await apiGet<CheckpointResultResponse[]>(
            `/interviews/${interviewId}/checkpoints`
          );
          const cp = checkpoints.find((c) => c.checkpointId === checkpointId);
          if (cp && cp.status !== "IN_PROGRESS") {
            pollingRef.current = false;

            const execution = {
              exitCode: cp.status === "PASSED" ? 0 : 1,
              stdout: cp.executionOutput ?? "",
              stderr: "",
              durationMs: 0,
              status: (cp.status === "PASSED" ? "SUCCESS" : "RUNTIME_ERROR") as
                | "SUCCESS"
                | "RUNTIME_ERROR",
            };
            applyCheckpointResult(cp, execution);

            if (cp.status === "PASSED") {
              await new Promise((r) => setTimeout(r, 1000));
              const nextCp = [...state.checkpoints]
                .sort((a, b) => a.sequenceNumber - b.sequenceNumber)
                .find(
                  (c) =>
                    c.checkpointId !== cp.checkpointId && c.status !== "PASSED"
                );
              if (nextCp) switchCheckpoint(nextCp.checkpointId);
            }
            return;
          }
        } catch {
          pollingRef.current = false;
          setRunning(false);
          return;
        }
      }
    },
    [state.checkpoints, setRunning, switchCheckpoint, applyCheckpointResult]
  );

  return { submit };
}
