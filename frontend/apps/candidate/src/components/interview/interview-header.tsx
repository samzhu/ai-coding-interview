"use client";

import { Button } from "@interview/shared/components/ui/button";
import { useInterview } from "@/contexts/interview-context";
import { InterviewTimer } from "./interview-timer";
import type { WorkspaceLayout } from "@/hooks/use-workspace-layout";
import { cn } from "@interview/shared/lib/utils";

interface InterviewHeaderProps {
  title: string;
  totalCheckpoints: number;
  onRun: () => void;
  onSubmit: () => void;
  isRunning: boolean;
  isSubmitting: boolean;
  layout: WorkspaceLayout;
  onToggleFileExplorer: () => void;
  onToggleRightPanel: () => void;
  onToggleTerminal: () => void;
  onResetLayout: () => void;
  interviewId: string;
  onExpired: () => void;
}

export function InterviewHeader({
  title,
  totalCheckpoints,
  onRun,
  onSubmit,
  isRunning,
  isSubmitting,
  layout,
  onToggleFileExplorer,
  onToggleRightPanel,
  onToggleTerminal,
  onResetLayout,
  interviewId,
  onExpired,
}: InterviewHeaderProps) {
  const { state } = useInterview();
  const currentSeq = state.checkpoint?.sequenceNumber ?? 1;
  const completedCount = currentSeq - 1;

  return (
    <header className="flex items-center gap-3 px-4 py-2 border-b border-[#333] bg-[#1e1e1e] shrink-0 min-w-0">
      {/* Title */}
      <h1
        className="font-semibold text-sm text-[#cccccc] truncate max-w-[180px] shrink-0"
        title={title}
      >
        {title}
      </h1>

      {/* Checkpoint Progress Rail */}
      <div className="flex items-center gap-1 shrink-0">
        {Array.from({ length: totalCheckpoints }).map((_, i) => {
          const isDone = i < completedCount;
          const isCurrent = i === completedCount;
          return (
            <span
              key={i}
              className={cn(
                "w-2.5 h-2.5 rounded-full transition-colors",
                isDone
                  ? "bg-emerald-400"
                  : isCurrent
                    ? "bg-[#007acc] ring-2 ring-[#007acc]/30"
                    : "bg-zinc-600"
              )}
            />
          );
        })}
        <span className="ml-1.5 text-xs text-[#858585]">
          {currentSeq}/{totalCheckpoints}
        </span>
      </div>

      {/* Divider */}
      <div className="w-px h-4 bg-[#444] shrink-0" />

      {/* Run + Submit buttons */}
      <div className="flex items-center gap-2 shrink-0">
        <Button
          variant="outline"
          size="sm"
          onClick={onRun}
          disabled={isRunning || isSubmitting || !state.checkpoint}
          className="h-7 text-xs bg-transparent border-[#555] text-[#cccccc] hover:bg-[#2d2d2d] hover:text-white disabled:opacity-40"
        >
          {isRunning ? "執行中..." : "▷ Run"}
        </Button>
        <Button
          size="sm"
          onClick={onSubmit}
          disabled={isSubmitting || isRunning || !state.checkpoint}
          className="h-7 text-xs bg-[#007acc] hover:bg-[#006bb3] text-white border-0 disabled:opacity-40"
        >
          {isSubmitting ? "提交中..." : "提交"}
        </Button>
      </div>

      {/* Timer — pushed to the right */}
      <div className="ml-auto shrink-0">
        <InterviewTimer interviewId={interviewId} onExpired={onExpired} />
      </div>

      {/* Divider */}
      <div className="w-px h-4 bg-[#444] shrink-0" />

      {/* Panel toggle buttons */}
      <div className="flex items-center gap-0.5 shrink-0">
        <button
          onClick={onToggleFileExplorer}
          title="切換檔案瀏覽器"
          className={cn(
            "px-2 py-1 text-sm rounded transition-colors",
            layout.fileExplorer
              ? "text-[#cccccc] bg-[#2d2d2d]"
              : "text-[#585858] hover:text-[#cccccc]"
          )}
        >
          📁
        </button>
        <button
          onClick={onToggleRightPanel}
          title="切換右側面板"
          className={cn(
            "px-2 py-1 text-sm rounded transition-colors",
            layout.rightPanel
              ? "text-[#cccccc] bg-[#2d2d2d]"
              : "text-[#585858] hover:text-[#cccccc]"
          )}
        >
          📋
        </button>
        <button
          onClick={onToggleTerminal}
          title="切換 Terminal"
          className={cn(
            "px-2 py-1 text-sm rounded transition-colors",
            layout.terminal
              ? "text-[#cccccc] bg-[#2d2d2d]"
              : "text-[#585858] hover:text-[#cccccc]"
          )}
        >
          ▶
        </button>
        <button
          onClick={onResetLayout}
          title="重置佈局"
          className="px-2 py-1 text-sm rounded text-[#585858] hover:text-[#cccccc] transition-colors"
        >
          ⊞
        </button>
      </div>
    </header>
  );
}
