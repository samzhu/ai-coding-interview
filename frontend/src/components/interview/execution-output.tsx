"use client";

import { useInterview } from "@/contexts/interview-context";

export function ExecutionOutput() {
  const { state } = useInterview();
  const { checkpoint, lastExecution } = state;

  // Show execution output from checkpoint or last execution
  const output = checkpoint?.executionOutput ?? lastExecution?.stdout ?? null;
  const stderr = lastExecution?.stderr;
  const status = checkpoint?.status;

  if (!output && !stderr) {
    return (
      <div className="flex items-center justify-center h-full text-sm text-muted-foreground">
        提交程式碼後，測試結果將顯示在此處
      </div>
    );
  }

  return (
    <div className="h-full overflow-y-auto p-3 space-y-3">
      {status && (
        <div
          className={`text-xs font-semibold px-2 py-1 rounded inline-block ${
            status === "PASSED"
              ? "bg-green-500/20 text-green-400"
              : status === "FAILED"
                ? "bg-red-500/20 text-red-400"
                : "bg-zinc-500/20 text-zinc-400"
          }`}
        >
          {status === "PASSED" ? "全部測試通過" : status === "FAILED" ? "測試失敗" : "執行中"}
        </div>
      )}

      {output && (
        <div>
          <p className="text-xs text-muted-foreground mb-1">輸出結果：</p>
          <pre className="text-xs font-mono bg-zinc-900 rounded p-2 whitespace-pre-wrap text-zinc-100">
            {output}
          </pre>
        </div>
      )}

      {stderr && (
        <div>
          <p className="text-xs text-red-400 mb-1">錯誤訊息：</p>
          <pre className="text-xs font-mono bg-red-950/50 rounded p-2 whitespace-pre-wrap text-red-300">
            {stderr}
          </pre>
        </div>
      )}
    </div>
  );
}
