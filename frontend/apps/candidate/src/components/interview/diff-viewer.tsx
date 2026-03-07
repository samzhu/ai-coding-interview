"use client";

import dynamic from "next/dynamic";

/**
 * DiffViewer — 多檔案 diff 視圖的動態載入包裝器
 *
 * 設計說明：
 * CodeMirror 使用 browser-only API，必須透過 dynamic import + ssr: false 載入，
 * 與 code-editor.tsx 的做法相同。
 * Map 在跨 dynamic boundary 傳遞時轉為 array（確保序列化相容性）。
 */

const DiffViewerInner = dynamic(() => import("./diff-viewer-inner"), {
  ssr: false,
  loading: () => (
    <div className="h-full bg-[#1e1e1e] flex items-center justify-center text-zinc-400 text-sm">
      載入 diff 視圖...
    </div>
  ),
});

interface DiffViewerProps {
  fileDiffs: Map<string, { originalFullContent: string; proposedFullContent: string }>;
}

export function DiffViewer({ fileDiffs }: DiffViewerProps) {
  const fileDiffsArray = Array.from(fileDiffs.entries()).map(
    ([filePath, { originalFullContent, proposedFullContent }]) => ({
      filePath,
      originalFullContent,
      proposedFullContent,
    })
  );

  return <DiffViewerInner fileDiffs={fileDiffsArray} />;
}
