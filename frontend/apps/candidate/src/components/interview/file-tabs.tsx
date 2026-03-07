"use client";

import { X, GitCompareArrows } from "lucide-react";
import { useInterview } from "@/contexts/interview-context";
import { cn } from "@interview/shared/lib/utils";

/**
 * FileTabs — 編輯器上方的 tab 列，包含一般檔案 tab 與獨立 Diff tab
 *
 * 設計說明：
 * - 當 state.diffReview 存在時，最前面自動出現特殊的「Diff」tab（橘色邊框）
 * - 點擊 Diff tab → setDiffTabActive(true)，編輯器區域切換為 DiffViewer
 * - 點擊一般 file tab → setDiffTabActive(false) + setActiveFile，顯示原始內容
 * - Diff tab 不可關閉（changeset 結束後自動消失）
 */
export function FileTabs() {
  const { state, setActiveFile, closeFile, setDiffTabActive } = useInterview();
  const { files, openFiles, activeFilePath, diffReview, diffTabActive } = state;

  const hasContent = openFiles.length > 0 || diffReview != null;
  if (!hasContent) return null;

  return (
    <div className="flex items-center gap-0 border-b overflow-x-auto shrink-0 bg-zinc-900">
      {/* 獨立 Diff tab：只在有 diffReview 時顯示 */}
      {diffReview != null && (
        <div
          className={cn(
            "group flex items-center gap-0 border-r border-zinc-700 shrink-0",
            diffTabActive
              ? "bg-zinc-800 border-b-2 border-b-orange-400"
              : "hover:bg-zinc-800/50"
          )}
        >
          <button
            onClick={() => setDiffTabActive(true)}
            className={cn(
              "flex items-center gap-1.5 px-3 py-1.5 text-xs whitespace-nowrap transition-colors",
              diffTabActive ? "text-orange-300" : "text-zinc-400 hover:text-zinc-200"
            )}
            title="檢視 AI 提案 diff"
          >
            <GitCompareArrows size={12} />
            <span>Diff</span>
          </button>
        </div>
      )}

      {/* 一般檔案 tab */}
      {openFiles.map((filePath) => {
        const file = files.get(filePath);
        if (!file) return null;
        const isActive = !diffTabActive && filePath === activeFilePath;
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
              onClick={() => {
                setDiffTabActive(false);
                setActiveFile(filePath);
              }}
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
