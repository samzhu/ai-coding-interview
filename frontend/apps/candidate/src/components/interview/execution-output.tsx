"use client";

import { useInterview } from "@/contexts/interview-context";

interface ExecutionOutputProps {
  onClose: () => void;
}

export function ExecutionOutput({ onClose }: ExecutionOutputProps) {
  const { state } = useInterview();
  const { checkpoint, lastExecution } = state;

  const output = checkpoint?.executionOutput ?? lastExecution?.stdout ?? null;
  const stderr = lastExecution?.stderr;
  const status = checkpoint?.status;

  return (
    <div
      className="flex flex-col border-t border-[#333] bg-[#1e1e1e] shrink-0"
      style={{ height: "192px" }}
    >
      {/* Header bar */}
      <div className="flex items-center px-3 py-1 bg-[#252526] border-b border-[#333] shrink-0">
        <span className="text-xs font-medium text-[#858585] uppercase tracking-wider">
          Terminal
        </span>
        <div className="flex-1" />
        <button
          onClick={onClose}
          className="text-[#858585] hover:text-[#cccccc] transition-colors text-sm px-1 leading-none"
          title="關閉 Terminal"
        >
          ✕
        </button>
      </div>

      {/* Output area */}
      <div className="flex-1 overflow-y-auto p-3 space-y-2">
        {!output && !stderr ? (
          <div className="flex items-center justify-center h-full text-xs text-[#585858]">
            提交程式碼後，測試結果將顯示在此處
          </div>
        ) : (
          <>
            {status && (
              <div
                className={`text-xs font-semibold px-2 py-1 rounded inline-block ${
                  status === "PASSED"
                    ? "bg-emerald-400/10 text-emerald-400"
                    : status === "FAILED"
                      ? "bg-red-400/10 text-red-400"
                      : "bg-zinc-500/20 text-zinc-400"
                }`}
              >
                {status === "PASSED"
                  ? "全部測試通過"
                  : status === "FAILED"
                    ? "測試失敗"
                    : "執行中"}
              </div>
            )}

            {output && (
              <pre className="text-xs font-mono text-[#4ec9b0] whitespace-pre-wrap leading-relaxed">
                {output}
              </pre>
            )}

            {stderr && (
              <pre className="text-xs font-mono text-[#f44747] whitespace-pre-wrap leading-relaxed">
                {stderr}
              </pre>
            )}
          </>
        )}
      </div>
    </div>
  );
}
