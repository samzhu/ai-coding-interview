"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import { apiPost, apiGet, ApiError } from "@interview/shared/lib/api-client";
import { useInterview } from "@/contexts/interview-context";
import type { CheckpointResultResponse } from "@interview/shared/types";

export function useCodeSubmission() {
  const router = useRouter();
  const { state, setSubmitting, setCheckpoint, applyCheckpointResult } =
    useInterview();

  const submit = useCallback(async () => {
    if (!state.checkpoint || state.isSubmitting) return;

    setSubmitting(true);

    try {
      // Files are already in the container via debounced writes
      const result = await apiPost<CheckpointResultResponse>(
        `/interviews/${state.interviewId}/checkpoints/${state.checkpoint.checkpointId}/submit`
      );

      if (result.status === "PASSED") {
        applyCheckpointResult(result, null);

        // Wait briefly then fetch the next checkpoint
        await new Promise((r) => setTimeout(r, 1000));

        try {
          const next = await apiGet<CheckpointResultResponse>(
            `/interviews/${state.interviewId}/checkpoints/current`
          );
          setCheckpoint(next);
        } catch (err) {
          if (err instanceof ApiError && err.status === 404) {
            // No more checkpoints — interview complete
            router.push(`/interview/${state.interviewId}/complete`);
          } else {
            throw err;
          }
        }
      } else {
        applyCheckpointResult(result, null);
      }
    } catch (err) {
      setSubmitting(false);
      throw err;
    }
  }, [state, setSubmitting, setCheckpoint, applyCheckpointResult, router]);

  return { submit };
}
