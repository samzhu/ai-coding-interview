"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@interview/shared/components/ui/button";
import { CodeEditor } from "@/components/code-editor/code-editor";
import { CheckpointPanel } from "./checkpoint-panel";
import { AiChatPanel } from "./ai-chat-panel";
import { ExecutionOutput } from "./execution-output";
import { FileTabs } from "./file-tabs";
import { InterviewTimer } from "./interview-timer";
import { useInterview } from "@/contexts/interview-context";
import { useCodeSubmission } from "@/hooks/use-code-submission";
import { useRunCode } from "@/hooks/use-run-code";
import { getApiUrl } from "@interview/shared/lib/api-client";

interface InterviewWorkspaceProps {
  interviewId: string;
  language: string;
}

export function InterviewWorkspace({
  interviewId,
  language,
}: InterviewWorkspaceProps) {
  const router = useRouter();
  const { state, setCode, setFileContent, getEditableFiles } = useInterview();
  const { submit } = useCodeSubmission();
  const { run } = useRunCode();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 5-second debounce code sync for interviewer monitoring
  useEffect(() => {
    if (debounceTimer.current) clearTimeout(debounceTimer.current);

    const isProjectMode =
      state.checkpoint?.projectFiles && state.checkpoint.projectFiles.length > 0;
    const filePath = isProjectMode ? state.activeFilePath ?? undefined : undefined;
    const code = state.code;

    if (!code) return;

    debounceTimer.current = setTimeout(() => {
      fetch(getApiUrl(`/interviews/${interviewId}/activity/code`), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ filePath: filePath ?? null, code }),
      }).catch(() => {
        // silent fail — monitoring is best-effort
      });
    }, 5000);

    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, [state.code, state.activeFilePath, interviewId]);

  const isProjectMode =
    state.checkpoint?.projectFiles && state.checkpoint.projectFiles.length > 0;

  const activeFile = state.activeFilePath
    ? state.files.get(state.activeFilePath)
    : null;
  const isReadOnly = activeFile ? !activeFile.editable : false;

  const handleExpired = useCallback(async () => {
    try {
      await submit();
    } catch {
      // 超時後提交可能被後端拒絕（410），直接導向完成頁
    }
    router.push(`/interview/${interviewId}/complete`);
  }, [submit, router, interviewId]);

  async function handleSubmit() {
    setSubmitError(null);
    try {
      await submit();
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "提交失敗，請稍後再試");
    }
  }

  async function handleRun() {
    setSubmitError(null);
    try {
      await run();
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "執行失敗，請稍後再試");
    }
  }

  function handleCodeChange(value: string) {
    if (isProjectMode && state.activeFilePath) {
      setFileContent(state.activeFilePath, value);
    } else {
      setCode(value);
    }
  }

  return (
    <div className="flex flex-col h-full overflow-hidden">
      {/* Timer bar */}
      <div className="flex items-center justify-end px-4 py-1 border-b bg-background shrink-0">
        <InterviewTimer interviewId={interviewId} onExpired={handleExpired} />
      </div>

      {/* Three-column layout */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left: Checkpoint panel (25%) */}
        <div className="w-1/4 border-r overflow-hidden flex flex-col">
          <CheckpointPanel />
        </div>

        {/* Center: Code editor (45%) */}
        <div className="flex-1 flex flex-col overflow-hidden border-r">
          {/* File tabs — only shown in project mode */}
          {isProjectMode && <FileTabs />}

          <div className="flex-1 overflow-hidden">
            <CodeEditor
              value={state.code}
              onChange={handleCodeChange}
              language={isProjectMode ? detectLanguage(state.activeFilePath) : language}
              readOnly={isReadOnly}
              className="h-full"
            />
          </div>

          {/* Editor action bar */}
          <div className="flex items-center gap-2 px-3 py-2 border-t bg-background shrink-0">
            {submitError && (
              <p className="text-xs text-destructive flex-1">{submitError}</p>
            )}
            <div className="flex gap-2 ml-auto">
              {isProjectMode && (
                <Button
                  variant="outline"
                  onClick={handleRun}
                  disabled={state.isRunning || state.isSubmitting || !state.checkpoint}
                  size="sm"
                >
                  {state.isRunning ? "執行中..." : "Run 測試"}
                </Button>
              )}
              <Button
                onClick={handleSubmit}
                disabled={state.isSubmitting || state.isRunning || !state.checkpoint}
                size="sm"
              >
                {state.isSubmitting ? "提交中..." : "提交"}
              </Button>
            </div>
          </div>
        </div>

        {/* Right: AI chat (30%) */}
        <div className="w-[30%] overflow-hidden flex flex-col">
          <AiChatPanel interviewId={interviewId} aiEnabled={state.aiEnabled} />
        </div>
      </div>

      {/* Bottom: Execution output */}
      <div className="h-40 border-t bg-zinc-950 overflow-hidden">
        <ExecutionOutput />
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
