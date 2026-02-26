"use client";

import { FolderOpen, FileText, Terminal, LayoutGrid } from "lucide-react";
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
      {/* 面試標題 */}
      <h1
        className="font-semibold text-sm text-[#cccccc] truncate max-w-[180px] shrink-0"
        title={title}
      >
        {title}
      </h1>

      {/* 彈性空間，將工具按鈕推到右側 */}
      <div className="flex-1" />

      {/* 工具列 — 加上中文文字 label，讓功能一目了然 */}
      <div className="flex items-center gap-0.5 shrink-0">
        <button
          onClick={onToggleFileExplorer}
          title="切換檔案瀏覽器"
          className={cn(
            "flex items-center gap-1 px-2 py-1 text-xs rounded transition-colors",
            layout.fileExplorer
              ? "text-[#cccccc] bg-[#2d2d2d]"
              : "text-[#585858] hover:text-[#cccccc]"
          )}
        >
          <FolderOpen className="w-3.5 h-3.5" />
          <span>檔案</span>
        </button>
        <button
          onClick={onToggleRightPanel}
          title="切換右側面板（題目 / AI 助理）"
          className={cn(
            "flex items-center gap-1 px-2 py-1 text-xs rounded transition-colors",
            layout.rightPanel
              ? "text-[#cccccc] bg-[#2d2d2d]"
              : "text-[#585858] hover:text-[#cccccc]"
          )}
        >
          <FileText className="w-3.5 h-3.5" />
          <span>題目</span>
        </button>
        <button
          onClick={onToggleTerminal}
          title="切換 Terminal"
          className={cn(
            "flex items-center gap-1 px-2 py-1 text-xs rounded transition-colors",
            layout.terminal
              ? "text-[#cccccc] bg-[#2d2d2d]"
              : "text-[#585858] hover:text-[#cccccc]"
          )}
        >
          <Terminal className="w-3.5 h-3.5" />
          <span>終端</span>
        </button>
        <button
          onClick={onResetLayout}
          title="重置佈局"
          className="flex items-center gap-1 px-2 py-1 text-xs rounded text-[#585858] hover:text-[#cccccc] transition-colors"
        >
          <LayoutGrid className="w-3.5 h-3.5" />
          <span>重置</span>
        </button>
      </div>

      {/* 分隔線 */}
      <div className="w-px h-4 bg-[#444] shrink-0" />

      {/* 計時器 — 放在最右側，作為時間壓力的視覺錨點 */}
      <div className="shrink-0">
        <InterviewTimer interviewId={interviewId} onExpired={onExpired} />
      </div>
    </header>
  );
}
