"use client";

import dynamic from "next/dynamic";

const CodeMirrorInner = dynamic(
  () => import("@/components/code-editor/code-mirror-inner"),
  { ssr: false }
);

interface MonitorCodeViewProps {
  code: string;
  filePath?: string;
  language?: string;
}

function detectLanguage(filePath?: string): string {
  if (!filePath) return "python";
  if (filePath.endsWith(".py")) return "python";
  if (filePath.endsWith(".js") || filePath.endsWith(".ts")) return "javascript";
  if (filePath.endsWith(".java")) return "java";
  return "python";
}

export function MonitorCodeView({
  code,
  filePath,
  language,
}: MonitorCodeViewProps) {
  const lang = language || detectLanguage(filePath);

  return (
    <div className="h-full flex flex-col">
      {filePath && (
        <div className="px-3 py-1.5 bg-muted/50 border-b text-xs text-muted-foreground font-mono shrink-0">
          {filePath}
        </div>
      )}
      <div className="flex-1 overflow-hidden">
        <CodeMirrorInner
          value={code}
          onChange={() => {}}
          language={lang}
          readOnly={true}
          className="h-full"
        />
      </div>
    </div>
  );
}
