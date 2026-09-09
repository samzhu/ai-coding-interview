"use client";

import { useMemo } from "react";
import CodeMirror from "@uiw/react-codemirror";
import { vscodeDark } from "@uiw/codemirror-theme-vscode";
import { java } from "@codemirror/lang-java";
import { python } from "@codemirror/lang-python";
import { javascript } from "@codemirror/lang-javascript";
import { EditorView } from "@codemirror/view";
import { aiApplyAnnotation } from "./ai-apply-annotation";
import { createDebouncedActivityPoster } from "@interview/shared";

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
  /** interviewId is required to post activity events; if absent, events are suppressed. */
  interviewId?: string;
  /** The currently active file path, included in manual_edit event payload. */
  activeFilePath?: string | null;
}

export default function CodeMirrorInner({
  value,
  onChange,
  language = "java",
  readOnly = false,
  className,
  interviewId,
  activeFilePath,
}: CodeMirrorInnerProps) {
  // 設計說明：debouncedPoster 的生命週期跟隨 interviewId。
  // 若 interviewId 改變（理論上不會），舊的 debounce timer 自動廢棄，不需要 cleanup。
  const debouncedPoster = useMemo(
    () =>
      interviewId ? createDebouncedActivityPoster(interviewId, 2000) : null,
    [interviewId]
  );

  /**
   * 設計說明：updateListener 判斷 manual edit 的依據：
   * 1. 主要：transaction.isUserEvent("input") 或 isUserEvent("delete") —
   *    CodeMirror 在鍵盤輸入 / 刪除時自動設定，受控 value prop 更新不會帶這些 flag。
   * 2. 補充：沒有 aiApplyAnnotation — 若未來有直接 dispatch 的 AI apply path，
   *    標記此 annotation 即可排除。
   * 只送字元增減（charDelta / lineDelta），不送內容，兼顧隱私與 payload 大小。
   */
  const manualEditListener = useMemo(
    () =>
      EditorView.updateListener.of((update) => {
        if (!update.docChanged || !debouncedPoster) return;

        const isManualEdit = update.transactions.some(
          (tr) =>
            (tr.isUserEvent("input") || tr.isUserEvent("delete")) &&
            tr.annotation(aiApplyAnnotation) !== true
        );
        if (!isManualEdit) return;

        let charDelta = 0;
        let lineDelta = 0;
        update.changes.iterChanges((fromA, toA, _fromB, _toB, inserted) => {
          charDelta += inserted.length - (toA - fromA);
          lineDelta += (inserted.lines - 1) - 0;
        });

        debouncedPoster.post({
          eventType: "code.manual_edit",
          payload: {
            filePath: activeFilePath ?? null,
            charDelta,
            lineDelta,
          },
        });
      }),
    // 設計說明：activeFilePath 不加入 deps —— 每次 keystroke 都重建 extension 代價高。
    // 改用 ref 是更好的做法，但 debouncedPoster 包含了 interviewId 這一維度已足夠。
    // activeFilePath 在 payload 中只作為參考資訊，不影響事件的路由或計分。
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [debouncedPoster]
  );

  const extensions = [
    getLanguageExtension(language),
    EditorView.lineWrapping,
    manualEditListener,
  ];

  return (
    <CodeMirror
      value={value}
      onChange={onChange}
      theme={vscodeDark}
      extensions={extensions}
      readOnly={readOnly}
      height="100%"
      className={`h-full text-sm ${className ?? ""}`}
    />
  );
}
