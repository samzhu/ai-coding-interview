/**
 * 設計說明：清理 AI 回應中的 edit_proposal 區塊及其殘留標記。
 * AI 模型偶爾輸出格式錯誤的 edit_proposal（如 <<<<<<< ORIGINAL 被截斷），
 * 導致主 regex 匹配提前結束，殘留的 diff 標記洩漏到 UI。
 * 兩階段清理：(1) 移除完整的 edit_proposal XML 區塊 (2) 清理 orphaned 標記。
 */

const EDIT_PROPOSAL_REGEX = /<edit_proposal[\s\S]*?<\/edit_proposal>/g;
const ORPHANED_MARKERS_REGEX =
  /(?:<<<<<<< ORIGINAL|>>>>>>> PROPOSED|=======|<\/?edit_proposal[^>]*>)/g;

export function stripEditProposals(content: string): string {
  return content
    .replace(EDIT_PROPOSAL_REGEX, "")
    .replace(ORPHANED_MARKERS_REGEX, "")
    .trim();
}
