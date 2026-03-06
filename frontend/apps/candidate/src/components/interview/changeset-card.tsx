"use client";

import { useState, useMemo } from "react";
import { CheckCircle, XCircle, FileCode } from "lucide-react";
import { toast } from "sonner";
import { apiPut } from "@interview/shared/lib/api-client";
import { useInterview } from "@/contexts/interview-context";
import { groupProposalsByFile, type EditProposalData } from "@/lib/changeset";

/**
 * ChangeSetCard — 聊天區批次變更提案卡片（取代舊的 EditProposalCard）
 *
 * 設計說明（Codex 風格批次審查）：
 * 同一 AI 回應的所有 proposals 合併為一張卡片，讓使用者一次性接受或拒絕所有變更。
 * 解決舊設計逐一套用時 original snippet 因共用 context 行已被修改而 exact match 失敗的問題。
 *
 * 狀態流程：
 * - pending：顯示檔案清單 + 同意/拒絕按鈕；編輯器已進入 diff 模式展示完整 diff
 * - applied：顯示綠色 badge「已套用」，摺疊
 * - rejected：顯示灰色 badge「已拒絕」，摺疊、半透明
 *
 * 同意流程：
 * 1. 從 state.diffReview.fileDiffs 讀取 proposedFullContent（REGISTER_CHANGESET 時已計算）
 * 2. apiPut 寫回後端（每個異動檔案各一次）
 * 3. setFileContent 更新本地 state（確保 code 與 files map 一致）
 * 4. dispatch APPLY_CHANGESET 標記完成並清除 diff 模式
 *
 * 拒絕流程：
 * dispatch REJECT_CHANGESET → reducer 從 fileDiffs.originalFullContent 還原檔案
 * 後端無需操作（proposed 內容從未寫入後端，REGISTER_CHANGESET 只改 state）
 */

interface ChangeSetCardProps {
  interviewId: string;
  messageId: string;
  proposals: EditProposalData[];
}

export function ChangeSetCard({ interviewId, messageId, proposals }: ChangeSetCardProps) {
  const { state, setFileContent, applyChangeSet, rejectChangeSet } = useInterview();
  const [isApplying, setIsApplying] = useState(false);

  // 從 state 讀取此 ChangeSet 的狀態（pending/applied/rejected）
  // 若 REGISTER_CHANGESET 尚未完成（useEffect 還沒跑），預設為 pending
  const changeSetStatus = state.changeSets.get(messageId)?.status ?? "pending";

  // 按檔案分組計算 fileChanges + change stats（記憶化）
  const fileChanges = useMemo(
    () => groupProposalsByFile(messageId, proposals).fileChanges,
    [messageId, proposals]
  );

  async function handleApply() {
    // 讀取 diffReview 確認 diff 模式已就緒（REGISTER_CHANGESET 已執行）
    const diffReview = state.diffReview;
    if (!diffReview || diffReview.changeSetMessageId !== messageId) {
      toast.error("無法套用：diff 模式尚未就緒，請稍後再試");
      return;
    }
    setIsApplying(true);
    try {
      for (const [filePath, { proposedFullContent }] of diffReview.fileDiffs) {
        await apiPut<void>(
          `/interviews/${interviewId}/files/content?path=${encodeURIComponent(filePath)}`,
          { content: proposedFullContent }
        );
        setFileContent(filePath, proposedFullContent);
      }
      applyChangeSet(messageId);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "套用失敗，請手動修改程式碼");
    } finally {
      setIsApplying(false);
    }
  }

  function handleReject() {
    rejectChangeSet(messageId);
  }

  // Applied 狀態：摺疊顯示綠色 badge
  if (changeSetStatus === "applied") {
    return (
      <div className="mt-2 w-full flex items-center gap-2 px-3 py-1.5 rounded-md border border-[#1e3a1e] bg-[#1a2e1a] text-xs">
        <CheckCircle className="w-3.5 h-3.5 text-[#4ec9b0] shrink-0" />
        <span className="text-[#4ec9b0] text-[11px]">程式碼變更已套用</span>
      </div>
    );
  }

  // Rejected 狀態：摺疊顯示灰色 badge，半透明
  if (changeSetStatus === "rejected") {
    return (
      <div className="mt-2 w-full flex items-center gap-2 px-3 py-1.5 rounded-md border border-[#333] bg-[#1e1e1e] text-xs opacity-50">
        <XCircle className="w-3.5 h-3.5 text-[#858585] shrink-0" />
        <span className="text-[#858585] text-[11px]">程式碼變更已拒絕</span>
      </div>
    );
  }

  // Pending 狀態：顯示完整卡片
  return (
    <div className="mt-2 w-full rounded-md border border-[#404040] bg-[#1e1e1e] overflow-hidden text-xs">
      {/* 標題列 */}
      <div className="flex items-center gap-2 px-3 py-2 bg-[#252526] border-b border-[#404040]">
        <span className="text-[#cccccc] font-medium text-[11px]">程式碼變更提案</span>
      </div>

      {/* 每個檔案一行：檔名 + change stats */}
      <div className="divide-y divide-[#2a2a2a]">
        {fileChanges.map((fc) => (
          <div key={fc.filePath} className="flex items-center gap-2 px-3 py-1.5">
            <FileCode className="w-3.5 h-3.5 text-[#858585] shrink-0" />
            <span className="text-[#569cd6] font-mono truncate flex-1 min-w-0 text-[11px]">
              {fc.filePath}
            </span>
            <div className="flex items-center gap-1.5 shrink-0 font-mono text-[11px]">
              <span className="text-[#4ec9b0]">+{fc.additions}</span>
              <span className="text-[#f48771]">-{fc.deletions}</span>
            </div>
          </div>
        ))}
      </div>

      {/* 操作按鈕列 */}
      <div className="flex items-center gap-2 px-3 py-2 border-t border-[#404040]">
        <button
          onClick={handleApply}
          disabled={isApplying}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded text-[11px] font-medium bg-[#0e7a0e] hover:bg-[#158015] text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <CheckCircle className="w-3 h-3" />
          {isApplying ? "套用中..." : "同意"}
        </button>
        <button
          onClick={handleReject}
          disabled={isApplying}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded text-[11px] font-medium bg-[#2d2d2d] hover:bg-[#3a3a3a] text-[#cccccc] border border-[#404040] disabled:opacity-50 transition-colors"
        >
          <XCircle className="w-3 h-3" />
          拒絕
        </button>
      </div>
    </div>
  );
}
