"use client";

import { useInterview } from "@/contexts/interview-context";

export function CheckpointPanel() {
  const { state } = useInterview();
  const { checkpoint } = state;

  if (!checkpoint) {
    return (
      <div className="p-4 text-sm text-[#858585] bg-[#1e1e1e] h-full">
        載入題目中...
      </div>
    );
  }

  const statusClass =
    checkpoint.status === "PASSED"
      ? "text-emerald-400 bg-emerald-400/10"
      : checkpoint.status === "FAILED"
        ? "text-red-400 bg-red-400/10"
        : "text-[#858585] bg-[#858585]/10";

  const statusLabel =
    checkpoint.status === "PASSED"
      ? "通過"
      : checkpoint.status === "FAILED"
        ? "未通過"
        : "待完成";

  return (
    <div className="h-full overflow-y-auto p-4 space-y-4 bg-[#1e1e1e]">
      <div className="flex items-center gap-2 flex-wrap">
        <h2 className="font-semibold text-sm text-[#cccccc]">{checkpoint.title}</h2>
        <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${statusClass}`}>
          {statusLabel}
        </span>
      </div>

      <div className="text-sm text-[#cccccc]/80 whitespace-pre-wrap leading-relaxed">
        {checkpoint.description}
      </div>
    </div>
  );
}
