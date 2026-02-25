"use client";

import { useState, useEffect } from "react";
import { Check } from "lucide-react";
import { Button } from "@interview/shared/components/ui/button";
import { useInterview } from "@/contexts/interview-context";
import { cn } from "@interview/shared/lib/utils";

interface CheckpointPanelProps {
  onRun: () => void;
  isRunning: boolean;
  onEndInterview: () => void;
}

export function CheckpointPanel({ onRun, isRunning, onEndInterview }: CheckpointPanelProps) {
  const { state } = useInterview();
  const { checkpoint, checkpoints } = state;

  const currentSeq = checkpoint?.sequenceNumber ?? 1;

  const allCheckpointsPassed =
    checkpoints.length > 0 &&
    currentSeq === checkpoints.length &&
    checkpoint?.status === "PASSED";

  const totalSteps = checkpoints.length + 1; // +1 虛擬結束節點
  const virtualNodeIndex = checkpoints.length;

  const [selectedIndex, setSelectedIndex] = useState(currentSeq - 1);

  // Auto-follow: 全部通過跳到虛擬節點，否則跟隨當前 checkpoint
  useEffect(() => {
    if (allCheckpointsPassed) {
      setSelectedIndex(virtualNodeIndex);
    } else {
      setSelectedIndex(currentSeq - 1);
    }
  }, [allCheckpointsPassed, virtualNodeIndex, currentSeq]);

  if (checkpoints.length === 0) {
    return (
      <div className="p-4 text-sm text-[#858585] bg-[#1e1e1e] h-full">
        載入題目中...
      </div>
    );
  }

  function getStepStatus(seqNumber: number): "PASSED" | "CURRENT" | "LOCKED" {
    if (seqNumber < currentSeq) return "PASSED";
    if (seqNumber === currentSeq) return "CURRENT";
    return "LOCKED";
  }

  const isVirtualNodeSelected = selectedIndex === virtualNodeIndex;
  const selected = isVirtualNodeSelected ? null : checkpoints[selectedIndex];
  const selectedSeq = selected?.sequenceNumber ?? 1;
  const selectedStatus = selected ? getStepStatus(selectedSeq) : null;

  // Title / description for real checkpoint
  const displayTitle =
    selectedStatus === "CURRENT" && checkpoint
      ? checkpoint.title
      : (selected?.title ?? "");
  const displayDescription =
    selectedStatus === "CURRENT" && checkpoint
      ? checkpoint.description
      : (selected?.description ?? "");

  // Badge for real checkpoint
  let badgeClass = "";
  let badgeLabel = "";
  if (!isVirtualNodeSelected && selectedStatus) {
    if (selectedStatus === "PASSED") {
      badgeClass = "text-emerald-400 bg-emerald-400/10";
      badgeLabel = "通過 ✓";
    } else if (selectedStatus === "CURRENT") {
      const s = checkpoint?.status;
      if (s === "PASSED") {
        badgeClass = "text-emerald-400 bg-emerald-400/10";
        badgeLabel = "通過";
      } else if (s === "FAILED") {
        badgeClass = "text-red-400 bg-red-400/10";
        badgeLabel = "未通過";
      } else {
        badgeClass = "text-[#858585] bg-[#858585]/10";
        badgeLabel = "待完成";
      }
    } else {
      badgeClass = "text-[#585858] bg-[#585858]/10";
      badgeLabel = "未開始";
    }
  }

  const isCurrentCheckpointSelected =
    !isVirtualNodeSelected && selectedIndex === currentSeq - 1;

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
            const stepStatus = getStepStatus(cp.sequenceNumber);
            const connectorClass =
              stepStatus === "PASSED" ? "bg-emerald-400" : "bg-zinc-600";

            return (
              <div key={cp.id} className="flex items-center flex-1">
                {/* Step circle */}
                <button
                  onClick={() => setSelectedIndex(i)}
                  title={cp.title}
                  className={cn(
                    "w-7 h-7 rounded-full flex items-center justify-center shrink-0 transition-all focus:outline-none",
                    stepStatus === "PASSED"
                      ? isSelected ? "bg-emerald-300" : "bg-emerald-400 hover:bg-emerald-300"
                      : stepStatus === "CURRENT"
                        ? isSelected ? "bg-[#0088dd]" : "bg-[#007acc] hover:bg-[#0088dd]"
                        : isSelected ? "bg-zinc-500" : "bg-zinc-600 hover:bg-zinc-500",
                    isSelected && "ring-2 ring-offset-1 ring-offset-[#1e1e1e]",
                    isSelected && stepStatus === "PASSED" && "ring-emerald-400",
                    isSelected && stepStatus === "CURRENT" && "ring-[#007acc]",
                    isSelected && stepStatus === "LOCKED" && "ring-zinc-400/60"
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

                {/* Connector to next step (including to virtual node) */}
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
                : "請先完成所有關卡。"}
            </div>
          </>
        ) : selected ? (
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

      {/* Bottom sticky bar — Run button (viewing current real checkpoint) */}
      {isCurrentCheckpointSelected && (
        <div className="shrink-0 border-t border-[#333] px-4 py-3 flex justify-end">
          <Button
            size="sm"
            onClick={onRun}
            disabled={isRunning || !state.checkpoint}
            className="h-7 text-xs bg-[#007acc] hover:bg-[#006bb3] text-white border-0 disabled:opacity-40"
          >
            {isRunning ? "執行中..." : "▷ Run"}
          </Button>
        </div>
      )}

      {/* Bottom sticky bar — 結束測試 button (viewing virtual end node) */}
      {isVirtualNodeSelected && (
        <div className="shrink-0 border-t border-[#333] px-4 py-3 flex justify-end">
          <Button
            size="sm"
            onClick={onEndInterview}
            disabled={!allCheckpointsPassed}
            className="h-7 text-xs bg-red-600/80 hover:bg-red-600 text-white border-0 disabled:opacity-40"
          >
            結束測試
          </Button>
        </div>
      )}
    </div>
  );
}
