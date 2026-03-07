"use client";

/**
 * xterm.js dynamic import wrapper（避免 SSR 錯誤）。
 *
 * 設計說明：
 * xterm.js 直接操作 window/document，在 Next.js SSR 期間會拋出 ReferenceError。
 * 使用 dynamic import + ssr:false 讓模組只在瀏覽器端載入。
 * loading fallback 顯示與終端背景色一致的佔位空間，避免 layout shift。
 */

import dynamic from "next/dynamic";

const XtermInner = dynamic(() => import("./xterm-terminal-inner"), {
  ssr: false,
  loading: () => (
    <div className="h-full w-full bg-[#1e1e1e] flex items-center justify-center">
      <span className="text-xs text-[#585858]">Connecting...</span>
    </div>
  ),
});

interface XtermTerminalProps {
  interviewId: string;
  isActive: boolean;
  initialCommand?: string;
}

export function XtermTerminal({ interviewId, isActive, initialCommand }: XtermTerminalProps) {
  return <XtermInner interviewId={interviewId} isActive={isActive} initialCommand={initialCommand} />;
}
