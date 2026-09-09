---
name: research
description: >-
  Research any development technology topic and produce structured tutorial
  documents. Use when the user wants to research, investigate, compare, learn
  about, or understand any technical topic including programming languages,
  frameworks, architecture patterns, DevOps, and cloud services. Trigger on:
  "research", "investigate", "compare", "analyze", "what is X", "how does X
  work", "X vs Y", "研究", "調查", "比較", "分析", "了解", "查一下", "幫我找".
  Also use when the user asks about a technology even without explicitly saying
  "research".
user-invocable: true
metadata:
  author: samzhu
  version: 1.2.0
  category: knowledge
  tags: [research, documentation, learning, tutorial]
---

# 技術研究與教學文件工作流程

## 概述

研究任何開發技術，並整理成可供學習的教學文件。特別適合開發新手，會主動引導學習方向。

```
┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
│ Phase 1 │──▶│ Phase 2 │──▶│ Phase 3 │──▶│ Phase 4 │──▶│ Phase 5 │
│  釐清   │   │  研究   │   │  決策   │   │  文件   │   │  引導   │
└─────────┘   └─────────┘   └─────────┘   └─────────┘   └─────────┘
     │             │             │             │             │
     ▼             ▼             ▼             ▼             ▼
  理解用戶       深度搜尋      報告發現      整理成        詢問後續
  真正需求       最新資料      等待選擇      教學文件      學習需求
```

---

## Phase 1：釐清需求

### 1.1 評估用戶的問題

用戶可能：
- 清楚知道要什麼 → 直接進入 Phase 2
- 只有模糊概念 → 需要協助釐清
- 完全是新手 → 需要更多引導

### 1.2 如果用戶講不清楚

1. **先快速搜尋**，了解相關主題
2. **列出可能的方向**，詢問用戶：
   ```markdown
   我搜尋了一下「[關鍵字]」，找到幾個相關方向：

   1. **[方向 A]** - 簡述...
   2. **[方向 B]** - 簡述...
   3. **[方向 C]** - 簡述...

   你想研究哪個方向？或者有其他具體的問題？
   ```
3. **等待用戶確認**後再進入深度研究

### 1.3 確認研究範圍

在開始深度研究前，確認：
- 研究的具體主題
- 用戶的背景/程度（新手/有經驗）
- 特定的使用場景（如果有）

---

## Phase 2：深度研究

### 2.1 搜尋策略

使用 WebSearch 進行多角度搜尋，**優先使用英文關鍵字**：

```
搜尋 1: [主題] official documentation {當前年份}     ← 官方文件優先
搜尋 2: [主題] best practices {當前年份}             ← 最佳實踐必找
搜尋 3: [主題] tutorial getting started
搜尋 4: [主題] vs [替代方案] comparison
搜尋 5: [主題] production tips / real world experience
```

### 2.2 主動尋找最佳實踐

每次研究都必須包含最佳實踐。搜尋關鍵字組合：

```
[主題] best practices {當前年份}
[主題] design patterns
[主題] common mistakes to avoid
[主題] anti-patterns
```

文件中必須包含的最佳實踐內容：
- ✅ 推薦的做法（Do's）
- ❌ 應避免的做法（Don'ts / Anti-patterns）
- 💡 實務經驗與技巧（Tips）
- ⚠️ 常見錯誤與陷阱（Common Mistakes）

### 2.3 資料來源優先順序

嚴格遵守：🥇 官方 > 🥈 大公司 > 🥉 英文文章 > 4️⃣ 社群 > 5️⃣ 中文

> 詳細分級表和來源範例見 `references/source-priority.md`

### 2.4 深入閱讀關鍵頁面

對於搜尋結果中的重要頁面（尤其是官方文件），使用 WebFetch 讀取完整內容：

```
CRITICAL: 不要只依賴搜尋摘要。對以下類型的頁面，必須用 WebFetch 取得完整內容：
- 官方 Getting Started / Quickstart 頁面
- 官方 Best Practices 頁面
- 版本 Migration Guide（如果涉及版本比較）
```

### 2.5 收集的資訊

- 核心概念與原理
- 優缺點分析
- 使用場景
- 入門步驟
- 常見陷阱
- 學習資源
- **所有來源的 URL**（重要！）

---

## Phase 3：報告與決策

### 3.1 報告格式

```markdown
## 研究結果：[主題名稱]

> 研究日期：YYYY-MM-DD

### 概述

[一段話說明這是什麼]

### 核心概念

1. **[概念 1]** - 說明...
2. **[概念 2]** - 說明...

### 方案比較（如果有多個選項）

| 比較項目 | 方案 A | 方案 B |
|----------|--------|--------|
| 優點 | ... | ... |
| 缺點 | ... | ... |
| 適用場景 | ... | ... |
| 學習曲線 | ... | ... |

### 建議

根據你的情況，建議...

### 來源

- [來源名稱](URL)
- [來源名稱](URL)

---

需要我深入研究哪個部分？或是選擇一個方向開始？
```

### 3.2 等待用戶決策

- 用戶可能選擇一個方向
- 用戶可能要求更多資訊
- 用戶可能改變方向

**不要假設，等待明確指示。**

---

## Phase 4：整理教學文件

### 4.1 文件位置與命名

```
docs/
├── INDEX.md                           ← 文件索引（必須更新！）
├── [category]/                        ← 分類目錄
│   └── [topic-name].md               ← 教學文件
```

**命名規則：**
- 使用小寫英文，單字間用連字號 `-`
- 例如：`event-sourcing.md`, `spring-boot-basics.md`

**分類流程：**

1. 讀取 `docs/INDEX.md` 的「分類說明」章節
2. 判斷文件應歸入哪個現有分類
3. 如果沒有適合的分類 → 詢問用戶是否新增
4. 新增分類後要同步更新 INDEX.md 的分類說明

### 4.2 教學文件結構

> 完整 Markdown 模板見 `references/document-template.md`

核心章節：簡介 → 核心概念 → 快速開始 → 最佳實踐 → 進階主題 → 常見問題 → 延伸學習 → 參考來源

### 4.3 更新索引

**每次新增或更新文件後，必須更新 `docs/INDEX.md`**

---

## Phase 5：引導後續學習

### 5.1 主動詢問

文件完成後，主動詢問用戶：

```markdown
---

文件已整理完成！

## 接下來你可能想了解...

根據 [剛才研究的主題]，以下是常見的後續學習方向：

1. **[相關主題 A]** - [為什麼相關]
2. **[相關主題 B]** - [為什麼相關]
3. **[相關主題 C]** - [為什麼相關]

你想繼續研究哪個方向？或是有其他問題？

（如果你不確定從哪裡開始，可以告訴我你想做什麼專案，我幫你規劃學習路線）
```

### 5.2 針對新手的引導

如果用戶是新手，可以主動：
- 解釋專有名詞
- 建議學習順序
- 提供入門資源
- 詢問具體想做的專案，反推需要學什麼

---

## Troubleshooting

### WebSearch 無結果或結果過少

1. 嘗試縮短關鍵字，去除年份限制
2. 用不同英文同義詞重試（例如 "event sourcing" vs "event-driven architecture"）
3. 改用更通用的上層概念搜尋，再從結果中縮小範圍

### 搜尋結果過時

1. 加上當前年份篩選：`[主題] {當前年份}`
2. 優先查看官方 changelog 或 release notes
3. 用 WebFetch 讀取官方文件頁面，確認最新版本號

### 用戶需求太廣泛

**不要直接開始全面研究。** 先用 Phase 1 的方向列表引導用戶縮小範圍，再進入 Phase 2。

### docs/INDEX.md 不存在

1. 詢問用戶是否要建立 `docs/` 目錄結構
2. 如果確認，建立 `docs/INDEX.md` 並加入分類說明章節

---

## Quality Checklist

在交付文件前，確認以下項目：

- [ ] 所有來源都附有可存取的 URL
- [ ] 至少包含一個官方來源（🥇）
- [ ] 最佳實踐章節包含 Do's 和 Don'ts
- [ ] 程式碼範例可直接執行（如適用）
- [ ] `docs/INDEX.md` 已更新
- [ ] 文件命名符合小寫連字號慣例

---

## Deep Dives

- `references/document-template.md` — 教學文件完整 Markdown 模板
- `references/source-priority.md` — 資料來源分級詳細表格與搜尋語言原則
