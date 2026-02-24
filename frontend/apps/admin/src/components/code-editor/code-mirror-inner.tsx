"use client";

import CodeMirror from "@uiw/react-codemirror";
import { vscodeDark } from "@uiw/codemirror-theme-vscode";
import { java } from "@codemirror/lang-java";
import { python } from "@codemirror/lang-python";
import { javascript } from "@codemirror/lang-javascript";
import { EditorView } from "@codemirror/view";

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
}

export default function CodeMirrorInner({
  value,
  onChange,
  language = "java",
  readOnly = false,
  className,
}: CodeMirrorInnerProps) {
  const extensions = [
    getLanguageExtension(language),
    EditorView.lineWrapping,
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
