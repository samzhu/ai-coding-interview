"use client";

import { useState } from "react";
import { CheckCircle, XCircle, ChevronDown, ChevronUp } from "lucide-react";
import { apiGet, apiPut } from "@interview/shared/lib/api-client";
import { useInterview } from "@/contexts/interview-context";
import type { FileContentResponse } from "@interview/shared/types";

export interface EditProposalData {
  type: "edit-proposal";
  filePath: string;
  original: string;
  proposed: string;
}

interface EditProposalCardProps {
  interviewId: string;
  proposal: EditProposalData;
}

type ProposalStatus = "pending" | "applying" | "applied" | "rejected" | "error";

/**
 * 渲染 AI 的程式碼修改提案，提供 diff 視圖與 Apply/Reject 操作。
 *
 * 設計說明（Human-in-the-loop）：
 * AI 沒有直接寫入檔案的工具，而是透過 edit_proposal 格式提案。
 * 候選人審查 diff 後決定是否套用，保留完整控制權。
 * Apply 流程：讀取當前檔案 → 替換程式碼 → 寫回 → 更新編輯器狀態。
 */
export function EditProposalCard({ interviewId, proposal }: EditProposalCardProps) {
  const [status, setStatus] = useState<ProposalStatus>("pending");
  const [errorMsg, setErrorMsg] = useState("");
  const [showDiff, setShowDiff] = useState(true);
  const { setFileContent } = useInterview();

  const { filePath, original, proposed } = proposal;

  async function handleApply() {
    setStatus("applying");
    setErrorMsg("");
    try {
      // 1. 讀取當前檔案內容，確認 original 仍存在（避免重複套用）
      const current = await apiGet<FileContentResponse>(
        `/interviews/${interviewId}/files/content?path=${encodeURIComponent(filePath)}`
      );
      if (!current.content.includes(original)) {
        setStatus("error");
        setErrorMsg("原始程式碼已變更，無法套用此提案。請重新向 AI 詢問。");
        return;
      }
      // 2. 計算新內容並寫入
      const newContent = current.content.replace(original, proposed);
      await apiPut<void>(
        `/interviews/${interviewId}/files/content?path=${encodeURIComponent(filePath)}`,
        { content: newContent }
      );
      // 3. 更新編輯器狀態（透過 context 的 setFileContent，CodeMirror 會重新渲染）
      setFileContent(current.filePath, newContent);
      setStatus("applied");
    } catch (err) {
      setStatus("error");
      setErrorMsg(err instanceof Error ? err.message : "套用失敗，請手動複製程式碼");
    }
  }

  function handleReject() {
    setStatus("rejected");
  }

  // 含 "applying" 狀態以便顯示 disabled 按鈕（"套用中..." 提示）
  const isPending = status === "pending" || status === "applying" || status === "error";

  return (
    <div className="mt-2 rounded-md border border-[#404040] bg-[#1a1a1a] overflow-hidden text-xs">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 bg-[#252526] border-b border-[#404040]">
        <div className="flex items-center gap-2 min-w-0">
          <span className="text-[#569cd6] font-mono truncate">{filePath}</span>
          {status === "applied" && (
            <span className="flex items-center gap-1 text-[#4ec9b0] shrink-0">
              <CheckCircle className="w-3.5 h-3.5" />
              已套用
            </span>
          )}
          {status === "rejected" && (
            <span className="flex items-center gap-1 text-[#858585] shrink-0">
              <XCircle className="w-3.5 h-3.5" />
              已拒絕
            </span>
          )}
        </div>
        <button
          onClick={() => setShowDiff((v) => !v)}
          className="text-[#858585] hover:text-[#cccccc] shrink-0 ml-2"
          aria-label={showDiff ? "收合 diff" : "展開 diff"}
        >
          {showDiff ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </button>
      </div>

      {/* Diff View */}
      {showDiff && (
        <div className="overflow-x-auto">
          <table className="w-full border-collapse font-mono text-[11px] leading-5">
            <tbody>
              {/* Original lines (red) */}
              {original.split("\n").map((line, i) => (
                <tr key={`orig-${i}`} className="bg-[#3d1515]">
                  <td className="px-2 py-0 text-[#858585] select-none w-4 text-right">-</td>
                  <td className="px-2 py-0 text-[#f48771] whitespace-pre">{line}</td>
                </tr>
              ))}
              {/* Proposed lines (green) */}
              {proposed.split("\n").map((line, i) => (
                <tr key={`prop-${i}`} className="bg-[#1a3d1a]">
                  <td className="px-2 py-0 text-[#858585] select-none w-4 text-right">+</td>
                  <td className="px-2 py-0 text-[#b5cea8] whitespace-pre">{line}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Error message */}
      {status === "error" && errorMsg && (
        <p className="px-3 py-1.5 text-[#f48771] border-t border-[#404040]">{errorMsg}</p>
      )}

      {/* Action buttons */}
      {isPending && (
        <div className="flex gap-2 px-3 py-2 border-t border-[#404040]">
          <button
            onClick={handleApply}
            disabled={status === "applying"}
            className="flex items-center gap-1.5 px-3 py-1 rounded text-[11px] font-medium bg-[#0e7a0e] hover:bg-[#158015] text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <CheckCircle className="w-3 h-3" />
            {status === "applying" ? "套用中..." : "套用"}
          </button>
          <button
            onClick={handleReject}
            className="flex items-center gap-1.5 px-3 py-1 rounded text-[11px] font-medium bg-[#2d2d2d] hover:bg-[#3a3a3a] text-[#cccccc] border border-[#404040] transition-colors"
          >
            <XCircle className="w-3 h-3" />
            拒絕
          </button>
        </div>
      )}
    </div>
  );
}
