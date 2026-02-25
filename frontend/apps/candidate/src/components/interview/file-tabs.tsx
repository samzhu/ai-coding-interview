"use client";

import { X } from "lucide-react";
import { useInterview } from "@/contexts/interview-context";
import { cn } from "@interview/shared/lib/utils";

export function FileTabs() {
  const { state, setActiveFile, closeFile } = useInterview();
  const { files, openFiles, activeFilePath } = state;

  if (openFiles.length === 0) return null;

  return (
    <div className="flex items-center gap-0 border-b overflow-x-auto shrink-0 bg-zinc-900">
      {openFiles.map((filePath) => {
        const file = files.get(filePath);
        if (!file) return null;
        const isActive = filePath === activeFilePath;
        const fileName = filePath.split("/").pop() ?? filePath;

        return (
          <div
            key={filePath}
            className={cn(
              "group flex items-center gap-0 border-r border-zinc-700 shrink-0",
              isActive
                ? "bg-zinc-800 border-b-2 border-b-blue-400"
                : "hover:bg-zinc-800/50"
            )}
          >
            <button
              onClick={() => setActiveFile(filePath)}
              className={cn(
                "flex items-center px-3 py-1.5 text-xs whitespace-nowrap transition-colors",
                isActive ? "text-white" : "text-zinc-400 hover:text-zinc-200"
              )}
              title={filePath}
            >
              <span>{fileName}</span>
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation();
                closeFile(filePath);
              }}
              className={cn(
                "flex items-center justify-center w-5 h-5 mr-1 rounded transition-colors",
                "opacity-0 group-hover:opacity-100",
                isActive ? "opacity-100" : "",
                "text-zinc-500 hover:text-zinc-200 hover:bg-zinc-700"
              )}
              title="關閉"
            >
              <X size={10} />
            </button>
          </div>
        );
      })}
    </div>
  );
}
