"use client";

import { useCallback } from "react";
import { apiPost } from "@interview/shared/lib/api-client";
import { useInterview } from "@/contexts/interview-context";
import type { CheckpointResultResponse } from "@interview/shared/types";

export function useRunCode() {
  const { state, setRunning, setExecution } = useInterview();

  const run = useCallback(async () => {
    if (!state.checkpoint || state.isRunning || state.isSubmitting) return;

    setRunning(true);

    try {
      // Files are already in the container via debounced writes
      const result = await apiPost<CheckpointResultResponse>(
        `/interviews/${state.interviewId}/checkpoints/${state.checkpoint.checkpointId}/run`
      );

      // Use executionOutput as a pseudo ExecutionResponse for display
      setExecution({
        exitCode: result.status === "PASSED" ? 0 : 1,
        stdout: result.executionOutput ?? "",
        stderr: "",
        durationMs: 0,
        status: result.status === "PASSED" ? "SUCCESS" : "RUNTIME_ERROR",
      });
    } catch (err) {
      setRunning(false);
      throw err;
    } finally {
      setRunning(false);
    }
  }, [state, setRunning, setExecution]);

  return { run };
}
