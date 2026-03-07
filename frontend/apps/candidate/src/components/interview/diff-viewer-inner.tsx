"use client";

import { useMemo } from "react";
import CodeMirror from "@uiw/react-codemirror";
import { vscodeDark } from "@uiw/codemirror-theme-vscode";
import { java } from "@codemirror/lang-java";
import { python } from "@codemirror/lang-python";
import { javascript } from "@codemirror/lang-javascript";
import { EditorView } from "@codemirror/view";
import { unifiedMergeView } from "@codemirror/merge";
import { FileCode } from "lucide-react";

/**
 * DiffViewerInner — 多檔案 diff 視圖（SSR-free，由 diff-viewer.tsx dynamic import 載入）
 *
 * 設計說明：
 * - 每個受影響的檔案各渲染一個 CodeMirror unifiedMergeView 區塊（original ↔ proposed）
 * - 使用與 code-mirror-inner.tsx 相同的 vscodeDark theme 確保視覺一致性
 * - 整個容器可垂直捲動，讓使用者一次看完所有 diff
 */

interface FileDiffEntry {
  filePath: string;
  originalFullContent: string;
  proposedFullContent: string;
}

interface DiffViewerInnerProps {
  fileDiffs: FileDiffEntry[];
}

function getLanguageExtension(filePath: string) {
  if (filePath.endsWith(".java")) return java();
  if (filePath.endsWith(".py")) return python();
  if (filePath.endsWith(".js") || filePath.endsWith(".ts")) return javascript();
  return python();
}

function FileDiffBlock({ filePath, originalFullContent, proposedFullContent }: FileDiffEntry) {
  const fileName = filePath.split("/").pop() ?? filePath;

  const extensions = useMemo(
    () => [
      getLanguageExtension(filePath),
      EditorView.lineWrapping,
      // mergeControls: false — 隱藏 per-chunk Accept/Reject 按鈕，
      // 避免與 ChangeSetCard 的同意/拒絕按鈕產生雙重操作混淆
      unifiedMergeView({
        original: originalFullContent,
        highlightChanges: true,
        gutter: true,
        mergeControls: false,
      }),
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [filePath, originalFullContent]
  );

  return (
    <div className="border-b border-[#333] last:border-b-0">
      {/* 檔案標題列 */}
      <div className="flex items-center gap-2 px-3 py-1.5 bg-[#252526] border-b border-[#333] sticky top-0 z-10">
        <FileCode className="w-3.5 h-3.5 text-[#858585] shrink-0" />
        <span className="text-[#569cd6] font-mono text-xs font-medium">{fileName}</span>
        {filePath !== fileName && (
          <span className="text-[#858585] font-mono text-[10px] truncate">{filePath}</span>
        )}
      </div>
      <CodeMirror
        value={proposedFullContent}
        theme={vscodeDark}
        extensions={extensions}
        readOnly
        className="text-sm"
      />
    </div>
  );
}

export default function DiffViewerInner({ fileDiffs }: DiffViewerInnerProps) {
  if (fileDiffs.length === 0) {
    return (
      <div className="h-full flex items-center justify-center text-zinc-500 text-sm">
        無檔案差異
      </div>
    );
  }

  return (
    <div className="h-full overflow-y-auto bg-[#1e1e1e]">
      {fileDiffs.map((entry) => (
        <FileDiffBlock key={entry.filePath} {...entry} />
      ))}
    </div>
  );
}
