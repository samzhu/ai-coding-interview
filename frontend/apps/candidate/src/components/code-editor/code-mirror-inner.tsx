"use client";

import CodeMirror from "@uiw/react-codemirror";
import { vscodeDark } from "@uiw/codemirror-theme-vscode";
import { java } from "@codemirror/lang-java";
import { python } from "@codemirror/lang-python";
import { javascript } from "@codemirror/lang-javascript";
import { EditorView } from "@codemirror/view";
import { unifiedMergeView } from "@codemirror/merge";

function getLanguageExtension(language: string) {
  switch (language.toLowerCase()) {
    case "java":
      return java();
    case "python":
      return python();
    case "javascript":
    case "typescript":
      return javascript({ typescript: language === "typescript" });
    default:
      return java();
  }
}

interface CodeMirrorInnerProps {
  value: string;
  onChange: (value: string) => void;
  language?: string;
  readOnly?: boolean;
  className?: string;
  // 提供 diffOriginal 時，編輯器進入 unified merge view 模式（VSCode 風格 inline diff）
  diffOriginal?: string | null;
}

export default function CodeMirrorInner({
  value,
  onChange,
  language = "java",
  readOnly = false,
  className,
  diffOriginal,
}: CodeMirrorInnerProps) {
  const extensions = [
    getLanguageExtension(language),
    EditorView.lineWrapping,
  ];

  if (diffOriginal != null) {
    // unified merge view：在同一編輯器中以刪除線（紅）+ 插入（綠）呈現 diff，
    // 類似 VSCode 的 inline diff 體驗，不需要切換分頁即可審查變更。
    // mergeControls: false — 隱藏 per-chunk Accept/Reject 按鈕，避免與 DiffReviewToolbar 操作衝突。
    // chunk 按鈕修改 CodeMirror 內部狀態但不通知 React context，會導致 toolbar 仍顯示而產生雙重操作混淆。
    extensions.push(unifiedMergeView({ original: diffOriginal, highlightChanges: true, gutter: true, mergeControls: false }));
  }

  return (
    <CodeMirror
      value={value}
      onChange={onChange}
      theme={vscodeDark}
      extensions={extensions}
      readOnly={readOnly || diffOriginal != null}
      height="100%"
      className={`h-full text-sm ${className ?? ""}`}
    />
  );
}
