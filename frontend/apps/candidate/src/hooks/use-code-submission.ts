"use client";

import { useCallback } from "react";
import { apiPost, apiGet, ApiError } from "@interview/shared/lib/api-client";
import { useInterview } from "@/contexts/interview-context";
import type { CheckpointResultResponse } from "@interview/shared/types";

export function useCodeSubmission() {
  const { state, setRunning, setCheckpoint, applyCheckpointResult } =
    useInterview();

  const submit = useCallback(async () => {
    if (!state.checkpoint || state.isRunning) return;

    setRunning(true);

    try {
      // Files are already in the container via debounced writes
      const result = await apiPost<CheckpointResultResponse>(
        `/interviews/${state.interviewId}/checkpoints/${state.checkpoint.checkpointId}/submit`
      );

      const execution = {
        exitCode: result.status === "PASSED" ? 0 : 1,
        stdout: result.executionOutput ?? "",
        stderr: "",
        durationMs: 0,
        status: (result.status === "PASSED" ? "SUCCESS" : "RUNTIME_ERROR") as
          | "SUCCESS"
          | "RUNTIME_ERROR",
      };

      if (result.status === "PASSED") {
        applyCheckpointResult(result, execution);

        // Wait briefly then fetch the next checkpoint
        await new Promise((r) => setTimeout(r, 1000));

        try {
          const next = await apiGet<CheckpointResultResponse>(
            `/interviews/${state.interviewId}/checkpoints/current`
          );
          setCheckpoint(next);
        } catch (err) {
          if (err instanceof ApiError && err.status === 404) {
            // All checkpoints passed — virtual "end test" step will guide the user
            return;
          } else {
            throw err;
          }
        }
      } else {
        applyCheckpointResult(result, execution);
      }
    } catch (err) {
      setRunning(false);
      throw err;
    }
  }, [state, setRunning, setCheckpoint, applyCheckpointResult]);

  return { submit };
}
