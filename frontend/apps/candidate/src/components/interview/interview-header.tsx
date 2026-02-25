"use client";

import { InterviewTimer } from "./interview-timer";
import type { WorkspaceLayout } from "@/hooks/use-workspace-layout";
import { cn } from "@interview/shared/lib/utils";

interface InterviewHeaderProps {
  title: string;
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
  layout,
  onToggleFileExplorer,
  onToggleRightPanel,
  onToggleTerminal,
  onResetLayout,
  interviewId,
  onExpired,
}: InterviewHeaderProps) {
  return (
    <header className="flex items-center gap-3 px-4 py-2 border-b border-[#333] bg-[#1e1e1e] shrink-0 min-w-0">
      {/* Title */}
      <h1
        className="font-semibold text-sm text-[#cccccc] truncate max-w-[180px] shrink-0"
        title={title}
      >
        {title}
      </h1>

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
