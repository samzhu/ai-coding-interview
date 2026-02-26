"use client";

import { useCallback } from "react";
import { apiPost } from "@interview/shared/lib/api-client";
import { useInterview } from "@/contexts/interview-context";
import type { CheckpointResultResponse } from "@interview/shared/types";

export function useCodeSubmission() {
  const { state, setRunning, switchCheckpoint, applyCheckpointResult } =
    useInterview();

  const submit = useCallback(async () => {
    if (!state.checkpoint || state.isRunning) return;

    setRunning(true);

    try {
      // 檔案已透過 debounced write 寫入 container，直接觸發評分
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

      // CHECKPOINT_RESULT reducer 會同步更新 state.checkpoint 和 state.checkpoints[] 的 status
      applyCheckpointResult(result, execution);

      if (result.status === "PASSED") {
        // 等待短暫時間讓用戶看到「通過」反饋
        await new Promise((r) => setTimeout(r, 1000));

        // 從本地 checkpoints[] 找下一個未通過的關卡（按 sequenceNumber 排序）
        // 不需要再打 /checkpoints/current API，直接從 state 計算
        // 注意：此時 state 仍是舊版（閉包），但 result.checkpointId 已知是剛通過的
        const nextCp = [...state.checkpoints]
          .sort((a, b) => a.sequenceNumber - b.sequenceNumber)
          .find(c => c.checkpointId !== result.checkpointId && c.status !== "PASSED");

        if (nextCp) {
          // 切換到下一個未完成的 checkpoint（SWITCH_CHECKPOINT reducer 從 checkpoints[] 找）
          switchCheckpoint(nextCp.checkpointId);
        }
        // 若 nextCp 不存在，代表全部通過，checkpoint-panel 的 allCheckpointsPassed 會自動跳到結束節點
      }
    } catch (err) {
      setRunning(false);
      throw err;
    }
  }, [state, setRunning, switchCheckpoint, applyCheckpointResult]);

  return { submit };
}
