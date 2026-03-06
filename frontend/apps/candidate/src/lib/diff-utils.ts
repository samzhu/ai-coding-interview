/**
 * diff-utils.ts — 計算兩段程式碼文字的新增/刪除行數統計，以及批次合併套用工具。
 *
 * 設計說明：
 * - computeChangeStats：使用 LCS（最長公共子序列）精確計算 unified diff 的 +/- 行數，
 *   而非單純比較行數差值。edit proposal 通常只有幾十行，O(n*m) 效能足夠。
 * - mergeProposals：批次合併套用多個 proposals，從尾部向頭部替換以避免位置偏移。
 *   使用 indexOf + slice 替換（非 String.replace），避免 $ 等特殊字元問題。
 */

export interface ChangeStats {
  additions: number;
  deletions: number;
}

/**
 * 以 LCS 演算法計算 original → proposed 的新增/刪除行數。
 * - deletions：出現在 original 但不在 LCS 的行（被移除）
 * - additions：出現在 proposed 但不在 LCS 的行（被新增）
 */
export function computeChangeStats(original: string, proposed: string): ChangeStats {
  const origLines = original.split("\n");
  const propLines = proposed.split("\n");

  const m = origLines.length;
  const n = propLines.length;

  // 建立 LCS 長度矩陣
  const dp: number[][] = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0));
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (origLines[i - 1] === propLines[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1;
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
      }
    }
  }

  const lcsLength = dp[m][n];
  return {
    deletions: m - lcsLength,
    additions: n - lcsLength,
  };
}

export interface MergeResult {
  result: string;
  /** 無法套用（original 找不到，或與已處理範圍重疊）的 proposal 數量 */
  skipped: number;
}

/**
 * 安全替換字串第一個出現位置（使用 indexOf + slice）。
 * 不使用 String.replace，避免 replacement 中 $1、$& 等特殊字元被誤解釋。
 */
export function replaceFirst(
  text: string,
  search: string,
  replacement: string
): string {
  const idx = text.indexOf(search);
  if (idx === -1) return text;
  return text.slice(0, idx) + replacement + text.slice(idx + search.length);
}

/**
 * 批次合併套用多個 proposals 到完整檔案內容。
 *
 * 演算法：
 * 1. 找到每個 proposal.original 在 fileContent 中的起始位置（indexOf）
 * 2. 按 startIndex 降序排列（從尾部開始替換，確保前方位置不受後方替換影響）
 * 3. 依序用 slice 替換（避免 String.replace 的 $ 特殊字元問題）
 * 4. 如果 original 找不到，跳過並累計 skipped
 * 5. 如果兩個 proposal 範圍重疊（當前 end > 已處理的 start），跳過後者並累計 skipped
 */
export function mergeProposals(
  fileContent: string,
  proposals: Array<{ original: string; proposed: string }>
): MergeResult {
  type Located = {
    original: string;
    proposed: string;
    start: number;
    end: number;
  };

  const located: Located[] = [];
  let skipped = 0;

  for (const p of proposals) {
    const idx = fileContent.indexOf(p.original);
    if (idx === -1) {
      console.warn("[mergeProposals] original not found, skipping:", p.original.slice(0, 60));
      skipped++;
      continue;
    }
    located.push({
      original: p.original,
      proposed: p.proposed,
      start: idx,
      end: idx + p.original.length,
    });
  }

  // 按起始位置降序排列（從尾部開始替換）
  located.sort((a, b) => b.start - a.start);

  let result = fileContent;
  // lastStart 記錄上一個已處理 proposal 的 start（在原始 fileContent 的座標）
  // 初始為 fileContent.length（末端），確保第一個 proposal 一定通過
  let lastStart = fileContent.length;

  for (const loc of located) {
    // 若此 proposal 的 end 超過上一個已處理的 start，則兩者重疊
    if (loc.end > lastStart) {
      console.warn("[mergeProposals] overlapping proposal, skipping:", loc.original.slice(0, 60));
      skipped++;
      continue;
    }
    // 由於降序處理，result.slice(0, loc.start) 的內容與 fileContent 的相同區段一致，
    // 因為所有已完成的替換都在 loc.start 之後（更高位置）
    result = result.slice(0, loc.start) + loc.proposed + result.slice(loc.end);
    lastStart = loc.start;
  }

  return { result, skipped };
}
