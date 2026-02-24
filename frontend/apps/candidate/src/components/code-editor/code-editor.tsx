"use client";

import dynamic from "next/dynamic";

// Dynamically import CodeMirror to avoid SSR issues
const CodeMirrorEditor = dynamic(() => import("./code-mirror-inner"), {
  ssr: false,
  loading: () => (
    <div className="h-full bg-[#1e1e1e] rounded-md flex items-center justify-center text-zinc-400 text-sm">
      載入編輯器...
    </div>
  ),
});

interface CodeEditorProps {
  value: string;
  onChange: (value: string) => void;
  language?: string;
  readOnly?: boolean;
  className?: string;
}

export function CodeEditor({
  value,
  onChange,
  language = "java",
  readOnly = false,
  className,
}: CodeEditorProps) {
  return (
    <CodeMirrorEditor
      value={value}
      onChange={onChange}
      language={language}
      readOnly={readOnly}
      className={className}
    />
  );
}
