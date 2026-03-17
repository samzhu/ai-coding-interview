"use client";

/**
 * VS Code 風格 tab 式終端面板。
 *
 * 設計說明：
 * - "測試結果" tab 永遠存在（顯示 ExecutionOutput 內容），不可關閉。
 * - bash tab 對應真實互動式終端（XtermTerminal），可新增和關閉。
 * - tab 切換時使用 CSS display:none 隱藏而非 unmount，保留 WebSocket 連線和終端緩衝。
 * - 因 xterm.js FitAddon 在 hidden 元素無法計算尺寸，isActive prop 通知 inner component 重新 fit。
 * - 每個 bash tab 使用時間戳作為唯一 ID，避免 React key 衝突。
 */

import { useState, useEffect, useRef } from "react";
import { XtermTerminal } from "./xterm-terminal";
import { useInterview } from "@/contexts/interview-context";
import { Terminal } from "@interview/shared/components/ai-elements/terminal";

interface TerminalPanelProps {
  interviewId: string;
  onClose: () => void;
  height: number;
  onResizeMouseDown: (e: React.MouseEvent) => void;
}

interface BashTab {
  id: string;
  label: string;
}

type ActiveTabId = "test-results" | string;

export function TerminalPanel({
  interviewId,
  onClose,
  height,
  onResizeMouseDown,
}: TerminalPanelProps) {
  const [activeTabId, setActiveTabId] = useState<ActiveTabId>("test-results");
  const [bashTabs, setBashTabs] = useState<BashTab[]>([]);
  const [bashCounter, setBashCounter] = useState(0);

  // 當 isRunning 從 false 變 true 時，自動切換到「測試結果」tab
  const { state } = useInterview();
  const prevRunningRef = useRef(false);
  useEffect(() => {
    if (state.isRunning && !prevRunningRef.current) {
      setActiveTabId("test-results");
    }
    prevRunningRef.current = state.isRunning;
  }, [state.isRunning]);

  function addBashTab() {
    const num = bashCounter + 1;
    const newTab: BashTab = {
      id: `bash-${Date.now()}`,
      label: `bash ${num}`,
    };
    setBashTabs((prev) => [...prev, newTab]);
    setBashCounter(num);
    setActiveTabId(newTab.id);
  }

  function closeBashTab(tabId: string, e: React.MouseEvent) {
    // 阻止事件冒泡，避免觸發 tab 切換
    e.stopPropagation();
    setBashTabs((prev) => prev.filter((t) => t.id !== tabId));
    // 若關閉的是當前 active tab，切換回測試結果
    if (activeTabId === tabId) {
      setActiveTabId("test-results");
    }
  }

  return (
    <>
      {/* 垂直拖拉把手 */}
      <div
        className="relative h-[5px] shrink-0 group cursor-row-resize z-10"
        onMouseDown={onResizeMouseDown}
      >
        <div className="absolute inset-x-0 top-[2px] h-[1px] bg-transparent group-hover:bg-[#007acc] transition-colors duration-100" />
      </div>

      <div
        className="flex flex-col border-t border-[#333] bg-[#1e1e1e] shrink-0"
        style={{ height }}
      >
        {/* Tab bar */}
        <div className="flex items-center bg-[#252526] border-b border-[#333] shrink-0 overflow-x-auto">
          {/* 測試結果 tab（固定，不可關閉） */}
          <button
            onClick={() => setActiveTabId("test-results")}
            className={`px-3 py-1.5 text-xs whitespace-nowrap border-r border-[#333] transition-colors ${
              activeTabId === "test-results"
                ? "text-white bg-[#1e1e1e] border-t border-t-[#007acc]"
                : "text-[#858585] hover:text-[#cccccc]"
            }`}
          >
            測試結果
          </button>

          {/* Bash terminal tabs */}
          {bashTabs.map((tab) => (
            <div
              key={tab.id}
              onClick={() => setActiveTabId(tab.id)}
              className={`flex items-center border-r border-[#333] cursor-pointer transition-colors ${
                activeTabId === tab.id
                  ? "bg-[#1e1e1e] border-t border-t-[#007acc]"
                  : "hover:bg-[#2d2d2d]"
              }`}
            >
              <span
                className={`pl-3 pr-1 py-1.5 text-xs whitespace-nowrap ${
                  activeTabId === tab.id ? "text-white" : "text-[#858585]"
                }`}
              >
                {tab.label}
              </span>
              <button
                onClick={(e) => closeBashTab(tab.id, e)}
                className="pr-2 py-1.5 text-[#585858] hover:text-[#cccccc] text-xs leading-none transition-colors"
                title="關閉終端"
              >
                ✕
              </button>
            </div>
          ))}

          {/* 新增 bash tab 按鈕 */}
          <button
            onClick={addBashTab}
            className="px-2.5 py-1.5 text-[#858585] hover:text-[#cccccc] text-sm transition-colors shrink-0"
            title="新增終端"
          >
            +
          </button>

          <div className="flex-1" />

          {/* 關閉整個 terminal panel */}
          <button
            onClick={onClose}
            className="px-2.5 py-1.5 text-[#858585] hover:text-[#cccccc] text-sm transition-colors shrink-0"
            title="關閉 Terminal"
          >
            ✕
          </button>
        </div>

        {/* 內容區 */}
        <div className="flex-1 overflow-hidden relative">
          {/* 測試結果 tab 內容 */}
          <div
            className="absolute inset-0 overflow-y-auto"
            style={{ display: activeTabId === "test-results" ? "flex" : "none", flexDirection: "column" }}
          >
            <TestResultsContent />
          </div>

          {/* Bash terminal tabs 內容 — hidden 而非 unmount，保留 WebSocket 連線 */}
          {bashTabs.map((tab) => (
            <div
              key={tab.id}
              className="absolute inset-0"
              style={{ display: activeTabId === tab.id ? "block" : "none" }}
            >
              <XtermTerminal
                interviewId={interviewId}
                isActive={activeTabId === tab.id}
              />
            </div>
          ))}

          {/* 無 tab 且非測試結果時的空狀態提示 */}
          {bashTabs.length === 0 && activeTabId !== "test-results" && (
            <div className="flex items-center justify-center h-full text-xs text-[#585858]">
              按 + 開啟新終端
            </div>
          )}
        </div>
      </div>
    </>
  );
}

/**
 * 測試結果顯示區。三種狀態：
 * 1. isRunning → 即時顯示 liveConsoleOutput（每 2 秒 polling 更新），使用 Terminal 元件
 * 2. 有結果 → 靜態顯示 PASSED/FAILED badge + output（Terminal 元件，isStreaming=false）
 * 3. 無結果 → placeholder 提示
 *
 * 設計說明：submit() 後後端非同步執行測試，前端透過 GET /executions/{processId} polling 取得
 * 累積的 consoleOutput。liveConsoleOutput 每次 polling 整批替換（非差量）。
 */
function TestResultsContent() {
  const { state } = useInterview();
  const { checkpoint, lastExecution, isRunning, liveConsoleOutput } = state;

  if (isRunning) {
    const liveOutput = liveConsoleOutput ?? "";
    return (
      <Terminal
        output={liveOutput || "等待測試輸出..."}
        isStreaming={true}
        autoScroll={true}
        className="flex-1 rounded-none border-0 border-none h-full"
      />
    );
  }

  const output = checkpoint?.executionOutput ?? lastExecution?.stdout ?? null;
  const status = checkpoint?.status;

  if (!output) {
    return (
      <div className="flex items-center justify-center flex-1 text-xs text-[#585858]">
        提交程式碼後，測試結果將顯示在此處
      </div>
    );
  }

  return (
    <div className="flex flex-col flex-1 overflow-hidden">
      {status && (
        <div className="px-3 pt-2 shrink-0">
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
        </div>
      )}
      <Terminal
        output={output}
        isStreaming={false}
        className="flex-1 rounded-none border-0 border-t border-zinc-800"
      />
    </div>
  );
}
