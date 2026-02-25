"use client";

import { useInterview } from "@/contexts/interview-context";
import { cn } from "@interview/shared/lib/utils";

export function FileTabs() {
  const { state, setActiveFile } = useInterview();
  const { files, activeFilePath } = state;

  if (files.size === 0) return null;

  return (
    <div className="flex items-center gap-0 border-b overflow-x-auto shrink-0 bg-zinc-900">
      {Array.from(files.values()).map((file) => {
        const isActive = file.filePath === activeFilePath;
        const fileName = file.filePath.split("/").pop() ?? file.filePath;

        return (
          <button
            key={file.filePath}
            onClick={() => setActiveFile(file.filePath)}
            className={cn(
              "flex items-center gap-1.5 px-3 py-1.5 text-xs border-r border-zinc-700 whitespace-nowrap transition-colors",
              isActive
                ? "bg-zinc-800 text-white border-b-2 border-b-blue-400"
                : "text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/50"
            )}
            title={file.filePath}
          >
            <span>{fileName}</span>
          </button>
        );
      })}
    </div>
  );
}
