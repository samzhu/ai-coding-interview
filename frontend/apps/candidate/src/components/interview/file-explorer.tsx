"use client";

import { useInterview } from "@/contexts/interview-context";
import { cn } from "@interview/shared/lib/utils";

interface FileExplorerProps {
  onCollapse: () => void;
}

function getFileIcon(filePath: string): string {
  if (filePath.endsWith(".py")) return "🐍";
  if (filePath.endsWith(".java")) return "☕";
  if (filePath.endsWith(".js") || filePath.endsWith(".mjs")) return "JS";
  if (filePath.endsWith(".ts")) return "TS";
  return "📄";
}

export function FileExplorer({ onCollapse }: FileExplorerProps) {
  const { state, setActiveFile } = useInterview();
  const { files, activeFilePath } = state;

  return (
    <div className="flex flex-col h-full bg-[#252526]">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 shrink-0">
        <span className="text-[10px] font-semibold tracking-widest text-[#858585] uppercase">
          Explorer
        </span>
        <button
          onClick={onCollapse}
          className="text-[#858585] hover:text-[#cccccc] transition-colors text-sm leading-none px-1"
          title="折疊檔案瀏覽器"
        >
          «
        </button>
      </div>

      {/* File list */}
      <div className="flex-1 overflow-y-auto">
        {files.size === 0 ? (
          <p className="text-[11px] text-[#585858] px-3 py-2">載入中...</p>
        ) : (
          Array.from(files.values()).map((file) => {
            const isActive = file.filePath === activeFilePath;
            const fileName = file.filePath.split("/").pop() ?? file.filePath;
            const icon = getFileIcon(file.filePath);

            return (
              <button
                key={file.filePath}
                onClick={() => setActiveFile(file.filePath)}
                className={cn(
                  "w-full flex items-center gap-2 px-3 py-1.5 text-xs transition-colors text-left",
                  isActive
                    ? "bg-[#37373d] text-[#cccccc] border-l-2 border-l-[#007acc]"
                    : "text-[#858585] hover:bg-[#2a2d2e] hover:text-[#cccccc] border-l-2 border-l-transparent"
                )}
                title={file.filePath}
              >
                <span className="shrink-0 text-[11px]">{icon}</span>
                <span className="truncate">{fileName}</span>
              </button>
            );
          })
        )}
      </div>
    </div>
  );
}
