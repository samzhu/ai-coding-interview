"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { CodeEditor } from "@/components/code-editor/code-editor";
import { ExecutionOutput } from "./execution-output";
import { FileTabs } from "./file-tabs";
import { FileExplorer } from "./file-explorer";
import { RightPanel } from "./right-panel";
import { InterviewHeader } from "./interview-header";
import { useInterview } from "@/contexts/interview-context";
import { useCodeSubmission } from "@/hooks/use-code-submission";
import { useRunCode } from "@/hooks/use-run-code";
import { useWorkspaceLayout } from "@/hooks/use-workspace-layout";
import { useWorkspaceFiles } from "@/hooks/use-workspace-files";

interface InterviewWorkspaceProps {
  interviewId: string;
  title: string;
  totalCheckpoints: number;
}

export function InterviewWorkspace({
  interviewId,
  title,
  totalCheckpoints,
}: InterviewWorkspaceProps) {
  const router = useRouter();
  const { state, setCode, setFileContent } = useInterview();
  const { submit } = useCodeSubmission();
  const { run } = useRunCode();
  const { layout, toggleFileExplorer, toggleRightPanel, toggleTerminal, resetLayout, autoExpandTerminal } =
    useWorkspaceLayout();
  const { saveFileDebounced } = useWorkspaceFiles(interviewId);

  const handleExpired = useCallback(async () => {
    try {
      await submit();
    } catch {
      // 超時後提交可能被後端拒絕，直接導向完成頁
    }
    router.push(`/interview/${interviewId}/complete`);
  }, [submit, router, interviewId]);

  async function handleSubmit() {
    try {
      await submit();
      autoExpandTerminal();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "提交失敗，請稍後再試");
      autoExpandTerminal();
    }
  }

  async function handleRun() {
    try {
      await run();
      autoExpandTerminal();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "執行失敗，請稍後再試");
      autoExpandTerminal();
    }
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
        totalCheckpoints={totalCheckpoints}
        onRun={handleRun}
        onSubmit={handleSubmit}
        isRunning={state.isRunning}
        isSubmitting={state.isSubmitting}
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
        {/* Left: File Explorer (collapsible) */}
        {layout.fileExplorer && (
          <div className="w-[180px] shrink-0 border-r border-[#333] overflow-hidden">
            <FileExplorer onCollapse={toggleFileExplorer} />
          </div>
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
                readOnly={false}
                className="h-full"
              />
            </div>
          </div>

          {/* Terminal: completely hidden or fully shown (VS Code style) */}
          {layout.terminal && <ExecutionOutput onClose={toggleTerminal} />}
        </div>

        {/* Right: Info Panel (full height, collapsible) */}
        {layout.rightPanel && (
          <div className="w-[30%] min-w-[280px] shrink-0 overflow-hidden flex flex-col">
            <RightPanel
              interviewId={interviewId}
              aiEnabled={state.aiEnabled}
              onClose={toggleRightPanel}
            />
          </div>
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
