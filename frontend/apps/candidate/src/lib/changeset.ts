/**
 * changeset.ts — ChangeSet 型別定義與工具函式
 *
 * 設計說明：同一 AI 回應中可能包含多個 edit_proposal（對同一或不同檔案）。
 * ChangeSet 將同一 messageId 的所有 proposals 合併為一個批次審查單元，
 * 讓使用者一次性接受或拒絕所有變更，避免逐一套用時 original snippet 已失效的問題。
 * 參考 Codex 的 changeset 審查流程設計。
 */

import { computeChangeStats } from "./diff-utils";

export interface EditProposalData {
  type: "edit-proposal";
  /** ToolCall.id — AI 模型產生的唯一 ID，供 Accept/Reject 時精確回傳後端定位 tool call arguments */
  proposalId?: string;
  filePath: string;
  original: string;
  proposed: string;
}

export interface FileChange {
  filePath: string;
  proposals: EditProposalData[];
  /** 累計此檔案所有 proposals 的新增行數 */
  additions: number;
  /** 累計此檔案所有 proposals 的刪除行數 */
  deletions: number;
}

export interface ChangeSet {
  messageId: string;
  status: "pending" | "applied" | "rejected";
  fileChanges: FileChange[];
}

/**
 * 將同一 AI 回應的所有 proposals 按檔案分組，並計算每個檔案的 change stats。
 * 使用 LCS 精確計算 +/- 行數，與 diff-utils.computeChangeStats 保持一致。
 */
export function groupProposalsByFile(
  messageId: string,
  proposals: EditProposalData[]
): ChangeSet {
  const fileMap = new Map<string, EditProposalData[]>();
  for (const p of proposals) {
    if (!fileMap.has(p.filePath)) fileMap.set(p.filePath, []);
    fileMap.get(p.filePath)!.push(p);
  }

  const fileChanges: FileChange[] = [];
  for (const [filePath, props] of fileMap) {
    let additions = 0;
    let deletions = 0;
    for (const p of props) {
      const stats = computeChangeStats(p.original, p.proposed);
      additions += stats.additions;
      deletions += stats.deletions;
    }
    fileChanges.push({ filePath, proposals: props, additions, deletions });
  }

  return { messageId, status: "pending", fileChanges };
}
