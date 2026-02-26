"use client";

import { useState, useEffect } from "react";
import { Check } from "lucide-react";
import { Button } from "@interview/shared/components/ui/button";
import { useInterview } from "@/contexts/interview-context";
import { cn } from "@interview/shared/lib/utils";
import type { CheckpointResultResponse } from "@interview/shared/types";

interface CheckpointPanelProps {
  onRun: () => void;
  isRunning: boolean;
  onEndInterview: () => void;
}

export function CheckpointPanel({ onRun, isRunning, onEndInterview }: CheckpointPanelProps) {
  const { state, switchCheckpoint } = useInterview();
  const { checkpoint, checkpoints } = state;

  // 資料驅動：所有 checkpoint 都 PASSED 才算全部完成
  const allCheckpointsPassed =
    checkpoints.length > 0 && checkpoints.every(c => c.status === "PASSED");

  const totalSteps = checkpoints.length + 1; // +1 虛擬結束節點
  const virtualNodeIndex = checkpoints.length;

  // selectedIndex 僅用於視覺 ring highlight；-1 = 尚未初始化
  const [selectedIndex, setSelectedIndex] = useState(-1);

  // 追蹤目前 active checkpoint 的 ID，供 auto-follow 同步 selectedIndex
  const activeCheckpointId = checkpoint?.checkpointId;

  // Auto-follow：當 active checkpoint 改變（包含初始載入、switchCheckpoint、提交通過後跳下一關）
  // 同步更新視覺選中的 stepper 節點
  useEffect(() => {
    if (allCheckpointsPassed) {
      // 全部通過 → 跳到虛擬結束節點
      setSelectedIndex(virtualNodeIndex);
    } else if (activeCheckpointId) {
      const idx = checkpoints.findIndex(c => c.checkpointId === activeCheckpointId);
      if (idx !== -1) setSelectedIndex(idx);
    }
  }, [allCheckpointsPassed, virtualNodeIndex, activeCheckpointId, checkpoints]);

  if (checkpoints.length === 0) {
    return (
      <div className="h-full flex flex-col bg-[#1e1e1e]">
        {/* Stepper — 只顯示虛擬結束節點 */}
        <div className="px-4 pt-4 pb-3 border-b border-[#333] shrink-0">
          <div className="flex items-center">
            <button
              title="結束測試"
              className="w-7 h-7 rounded-full flex items-center justify-center shrink-0 bg-zinc-500 ring-2 ring-offset-1 ring-offset-[#1e1e1e] ring-zinc-400/60 focus:outline-none"
            >
              <span className="text-[13px] text-white leading-none select-none">⏹</span>
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="p-4 space-y-3 overflow-y-auto flex-1">
          <h2 className="font-semibold text-sm text-[#cccccc]">結束測試</h2>
          <div className="text-sm text-[#cccccc]/80 leading-relaxed">
            考試內容由面試官確認，您可以隨時結束測試。
          </div>
        </div>

        {/* Bottom — 結束測試按鈕 */}
        <div className="shrink-0 border-t border-[#333] px-4 py-3 flex justify-end">
          <Button
            size="sm"
            onClick={onEndInterview}
            className="h-7 text-xs bg-red-600/80 hover:bg-red-600 text-white border-0"
          >
            結束測試
          </Button>
        </div>
      </div>
    );
  }

  // 判斷 step 狀態：資料驅動，取代舊版的順序驅動（PASSED/CURRENT/LOCKED）
  // 現在只有 PASSED（綠）和 PENDING（灰），可點擊跳選
  function getStepStatus(cp: CheckpointResultResponse): "PASSED" | "PENDING" {
    return cp.status === "PASSED" ? "PASSED" : "PENDING";
  }

  const isVirtualNodeSelected = selectedIndex === virtualNodeIndex;
  // 目前顯示內容直接取自 active checkpoint（已由 switchCheckpoint 同步）
  const displayTitle = checkpoint?.title ?? "";
  const displayDescription = checkpoint?.description ?? "";
  const activeStatus = checkpoint?.status;

  // 計算 badge（根據 active checkpoint 的狀態）
  let badgeClass = "";
  let badgeLabel = "";
  if (!isVirtualNodeSelected && checkpoint) {
    if (activeStatus === "PASSED") {
      badgeClass = "text-emerald-400 bg-emerald-400/10";
      badgeLabel = "通過 ✓";
    } else if (activeStatus === "FAILED") {
      badgeClass = "text-red-400 bg-red-400/10";
      badgeLabel = "未通過";
    } else {
      badgeClass = "text-[#858585] bg-[#858585]/10";
      badgeLabel = "待完成";
    }
  }

  // Run 按鈕顯示條件：選中的是真實 checkpoint 且尚未通過
  const showRunButton = !isVirtualNodeSelected && activeStatus !== "PASSED";

  return (
    <div className="h-full flex flex-col bg-[#1e1e1e]">
      {/* Stepper */}
      <div className="px-4 pt-4 pb-3 border-b border-[#333] shrink-0">
        <div className="flex items-center">
          {Array.from({ length: totalSteps }).map((_, i) => {
            const isVirtual = i === virtualNodeIndex;
            const isLastItem = i === totalSteps - 1;
            const isSelected = i === selectedIndex;

            if (isVirtual) {
              const virtualActive = allCheckpointsPassed;
              return (
                <div key="virtual-end" className="flex items-center flex-none">
                  <button
                    onClick={() => setSelectedIndex(virtualNodeIndex)}
                    title="結束測試"
                    className={cn(
                      "w-7 h-7 rounded-full flex items-center justify-center shrink-0 transition-all focus:outline-none",
                      virtualActive
                        ? isSelected ? "bg-red-500" : "bg-red-600/80 hover:bg-red-600"
                        : isSelected ? "bg-zinc-500" : "bg-zinc-600 hover:bg-zinc-500",
                      isSelected && "ring-2 ring-offset-1 ring-offset-[#1e1e1e]",
                      isSelected && virtualActive && "ring-red-500/60",
                      isSelected && !virtualActive && "ring-zinc-400/60"
                    )}
                  >
                    <span className="text-[13px] text-white leading-none select-none">⏹</span>
                  </button>
                </div>
              );
            }

            const cp = checkpoints[i];
            const stepStatus = getStepStatus(cp);
            // connector 顏色：該 step 已 PASSED 則顯示綠色
            const connectorClass =
              stepStatus === "PASSED" ? "bg-emerald-400" : "bg-zinc-600";

            return (
              <div key={cp.checkpointId} className="flex items-center flex-1">
                {/* Step circle — 點擊可自由跳選，呼叫 switchCheckpoint 切換 active checkpoint */}
                <button
                  onClick={() => {
                    setSelectedIndex(i);
                    switchCheckpoint(cp.checkpointId);
                  }}
                  title={cp.title}
                  className={cn(
                    "w-7 h-7 rounded-full flex items-center justify-center shrink-0 transition-all focus:outline-none",
                    stepStatus === "PASSED"
                      ? isSelected ? "bg-emerald-300" : "bg-emerald-400 hover:bg-emerald-300"
                      : isSelected ? "bg-[#0088dd]" : "bg-zinc-600 hover:bg-zinc-500",
                    isSelected && "ring-2 ring-offset-1 ring-offset-[#1e1e1e]",
                    isSelected && stepStatus === "PASSED" && "ring-emerald-400",
                    isSelected && stepStatus === "PENDING" && "ring-[#007acc]"
                  )}
                >
                  {stepStatus === "PASSED" ? (
                    <Check className="w-3.5 h-3.5 text-white" strokeWidth={3} />
                  ) : (
                    <span className="text-[13px] font-medium text-white leading-none select-none">
                      {cp.sequenceNumber}
                    </span>
                  )}
                </button>

                {/* Connector to next step */}
                {!isLastItem && (
                  <div className={cn("flex-1 h-[2px] mx-1", connectorClass)} />
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Content area */}
      <div className="p-4 space-y-3 overflow-y-auto flex-1">
        {isVirtualNodeSelected ? (
          <>
            <h2 className="font-semibold text-sm text-[#cccccc]">結束測試</h2>
            <div className="text-sm text-[#cccccc]/80 leading-relaxed">
              {allCheckpointsPassed
                ? "所有關卡都已通過！確認完成後，請按下方按鈕結束測試。"
                : "您可以先完成其他關卡，或直接結束測試。"}
            </div>
          </>
        ) : checkpoint ? (
          <>
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="font-semibold text-sm text-[#cccccc]">{displayTitle}</h2>
              <span className={cn("text-xs px-1.5 py-0.5 rounded font-medium", badgeClass)}>
                {badgeLabel}
              </span>
            </div>
            <div className="text-sm text-[#cccccc]/80 whitespace-pre-wrap leading-relaxed">
              {displayDescription}
            </div>
          </>
        ) : null}
      </div>

      {/* Bottom sticky bar — Run 按鈕（選中任意非 PASSED checkpoint 時顯示）*/}
      {showRunButton && (
        <div className="shrink-0 border-t border-[#333] px-4 py-3 flex justify-end">
          <Button
            size="sm"
            onClick={onRun}
            disabled={isRunning || !checkpoint}
            className="h-7 text-xs bg-[#007acc] hover:bg-[#006bb3] text-white border-0 disabled:opacity-40"
          >
            {isRunning ? "執行中..." : "▷ Run"}
          </Button>
        </div>
      )}

      {/* Bottom sticky bar — 結束測試按鈕（虛擬結束節點時顯示，永遠可點擊）*/}
      {isVirtualNodeSelected && (
        <div className="shrink-0 border-t border-[#333] px-4 py-3 flex justify-end">
          <Button
            size="sm"
            onClick={onEndInterview}
            className="h-7 text-xs bg-red-600/80 hover:bg-red-600 text-white border-0"
          >
            結束測試
          </Button>
        </div>
      )}
    </div>
  );
}
