"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { apiPost } from "@interview/shared/lib/api-client";
import { CodeEditor } from "@/components/code-editor/code-editor";
import { TerminalPanel } from "./terminal-panel";
import { FileTabs } from "./file-tabs";
import { FileExplorer } from "./file-explorer";
import { RightPanel } from "./right-panel";
import { InterviewHeader } from "./interview-header";
import { useInterview } from "@/contexts/interview-context";
import { useCodeSubmission } from "@/hooks/use-code-submission";
import { useWorkspaceLayout } from "@/hooks/use-workspace-layout";
import { useWorkspaceFiles } from "@/hooks/use-workspace-files";
import { useResizablePanel } from "@/hooks/use-resizable-panel";
import { useResizableHeight } from "@/hooks/use-resizable-height";

interface InterviewWorkspaceProps {
  interviewId: string;
  title: string;
}

export function InterviewWorkspace({
  interviewId,
  title,
}: InterviewWorkspaceProps) {
  const router = useRouter();
  const { state, setCode, setFileContent } = useInterview();
  const { submit } = useCodeSubmission();
  const { layout, toggleFileExplorer, toggleRightPanel, toggleTerminal, resetLayout, autoExpandTerminal } =
    useWorkspaceLayout();
  const { saveFileDebounced } = useWorkspaceFiles(interviewId);
  const explorerResize = useResizablePanel({ initialWidth: 180, minWidth: 120, maxWidth: 480 });
  const rightPanelResize = useResizablePanel({ initialWidth: 320, minWidth: 240, maxWidth: 600, reversed: true });
  const terminalResize = useResizableHeight({ initialHeight: 192, minHeight: 100, maxHeight: 500 });

  // 計算目前 active 檔案的 diffOriginal（多檔案 diff 設計）：
  // 若 activeFilePath 是此 ChangeSet 中有 diff 的檔案，顯示對應的 originalFullContent；
  // 否則為 null（不進入 diff 模式）。這讓使用者可透過 FileTabs 切換查看不同檔案的 diff。
  const diffOriginal = state.diffReview
    ? (state.diffReview.fileDiffs.get(state.activeFilePath ?? "")?.originalFullContent ?? null)
    : null;

  const handleExpired = useCallback(async () => {
    try {
      await apiPost(`/interviews/${interviewId}/complete`);
    } catch {
      // 超時後呼叫可能被後端拒絕，直接導向完成頁
    }
    router.push(`/interview/${interviewId}/complete`);
  }, [interviewId, router]);

  async function handleRun() {
    try {
      await submit();
      autoExpandTerminal();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "執行失敗，請稍後再試");
      autoExpandTerminal();
    }
  }

  async function handleEndInterview() {
    const currentSeq = state.checkpoint?.sequenceNumber ?? 1;
    const total = state.checkpoints.length;
    const passed = currentSeq - 1;
    const allPassed =
      total > 0 &&
      state.checkpoint?.status === "PASSED" &&
      state.checkpoint?.sequenceNumber === total;
    if (!allPassed && passed < total) {
      const confirmed = window.confirm(
        `還有 ${total - passed} 個關卡未完成，確定要結束測試嗎？`
      );
      if (!confirmed) return;
    }
    try {
      await apiPost(`/interviews/${interviewId}/complete`);
    } catch {
      // ignore
    }
    router.push(`/interview/${interviewId}/complete`);
  }

  function handleCodeChange(value: string) {
    if (state.activeFilePath) {
      setFileContent(state.activeFilePath, value);
      saveFileDebounced(state.activeFilePath, value);
    } else {
      setCode(value);
    }
  }

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[#1e1e1e] dark">
      {/* Header — full-width control bar */}
      <InterviewHeader
        title={title}
        layout={layout}
        onToggleFileExplorer={toggleFileExplorer}
        onToggleRightPanel={toggleRightPanel}
        onToggleTerminal={toggleTerminal}
        onResetLayout={resetLayout}
        interviewId={interviewId}
        onExpired={handleExpired}
      />

      {/* Main area: three columns */}
      <div className="flex flex-1 overflow-hidden min-h-0">
        {/* Left: File Explorer (collapsible, resizable) */}
        {layout.fileExplorer && (
          <>
            <div
              className="shrink-0 border-r border-[#333] overflow-hidden"
              style={{ width: explorerResize.width }}
            >
              <FileExplorer onCollapse={toggleFileExplorer} />
            </div>
            {/* Drag handle */}
            <div
              className="relative w-[5px] shrink-0 group cursor-col-resize z-10"
              onMouseDown={explorerResize.onMouseDown}
            >
              <div className="absolute inset-y-0 left-[2px] w-[1px] bg-transparent group-hover:bg-[#007acc] transition-colors duration-100" />
            </div>
          </>
        )}

        {/* Center: Editor + Terminal (stacked vertically) */}
        <div className="flex-1 flex flex-col overflow-hidden min-w-0">
          {/* Editor zone */}
          <div className="flex-1 flex flex-col overflow-hidden border-r border-[#333]">
            <FileTabs />
            <div className="flex-1 overflow-hidden">
              <CodeEditor
                value={state.code}
                onChange={handleCodeChange}
                language={detectLanguage(state.activeFilePath)}
                readOnly={state.diffReview != null}
                className="h-full"
                diffOriginal={diffOriginal}
              />
            </div>
          </div>

          {/* Terminal Panel：VS Code 風格 tab 式終端（測試結果 + 互動式 bash） */}
          {layout.terminal && (
            <TerminalPanel
              interviewId={interviewId}
              onClose={toggleTerminal}
              height={terminalResize.height}
              onResizeMouseDown={terminalResize.onMouseDown}
            />
          )}
        </div>

        {/* Right: Info Panel (full height, collapsible, resizable) */}
        {layout.rightPanel && (
          <>
            {/* Drag handle */}
            <div
              className="relative w-[5px] shrink-0 group cursor-col-resize z-10"
              onMouseDown={rightPanelResize.onMouseDown}
            >
              <div className="absolute inset-y-0 left-[2px] w-[1px] bg-transparent group-hover:bg-[#007acc] transition-colors duration-100" />
            </div>
            <div
              className="shrink-0 overflow-hidden flex flex-col"
              style={{ width: rightPanelResize.width }}
            >
              <RightPanel
                interviewId={interviewId}
                aiEnabled={state.aiEnabled}
                onClose={toggleRightPanel}
                onRun={handleRun}
                isRunning={state.isRunning}
                onEndInterview={handleEndInterview}
              />
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function detectLanguage(filePath: string | null): string {
  if (!filePath) return "python";
  if (filePath.endsWith(".py")) return "python";
  if (filePath.endsWith(".js") || filePath.endsWith(".ts")) return "javascript";
  if (filePath.endsWith(".java")) return "java";
  return "python";
}
