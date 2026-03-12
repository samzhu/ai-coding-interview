---
name: bdd
description: >-
  Behavior-Driven Development workflow covering Discovery, Formulation, and
  Automation phases. Use when the user explores requirements with examples,
  writes Gherkin scenarios, does Example Mapping, defines acceptance criteria,
  discusses user stories, or bridges business and technical understanding.
  Trigger on: "BDD", "Gherkin", "scenario", "feature file", ".feature",
  "acceptance criteria", "user story", "Example Mapping", "Three Amigos",
  "需求", "驗收", "使用者故事", "功能規格", "行為驅動". Also use when discussing
  business rules or translating requirements into testable specifications.
metadata:
  author: samzhu
  version: 1.1.0
---

# BDD — 行為驅動開發工作流程

## Overview

BDD 是協作實踐：透過具體範例建立共識，再將範例轉化為可執行的規格。

```
Discovery ──▶ Formulation ──▶ Automation
  探索需求       撰寫場景        自動化驗證
```

**核心原則：先對話、用範例、再寫碼。**

---

## Phase 1：Discovery — 探索需求

目標：透過協作對話，用具體範例釐清需求，消除模糊。

### 1.1 三視角探索

每個功能從三個角度檢視：

| 視角 | 核心問題 |
|------|----------|
| **業務** | 解決什麼問題？價值是什麼？業務規則？ |
| **開發** | 怎麼實作？技術限制？邊界案例？ |
| **測試** | 什麼會出錯？遺漏了什麼？怎麼驗證？ |

**流程：**

1. 業務方描述功能需求
2. 三個視角輪流提問、挑戰假設
3. 用具體範例回答每個問題
4. 記錄所有範例和未解決的疑問

### 1.2 Example Mapping（四色卡片法）

將探索結果結構化：

| 顏色 | 代表 | 說明 |
|------|------|------|
| 🟡 黃色 | Story | 要實作的功能 |
| 🔵 藍色 | Rule | 驗收條件 / 業務規則 |
| 🟢 綠色 | Example | 具體情境（變成 scenario） |
| 🔴 紅色 | Question | 未解決的疑問 |

**操作步驟：**

1. 頂部放一張黃色卡片（Story）
2. 列出已知的藍色規則
3. 每條規則下方列出綠色範例
4. 任何不確定的用紅色標記

**完成判定：**

- 紅色卡片為零或已有明確處理方式
- 每條規則至少有一個正面和一個反面範例
- 三個視角都同意範例充分

### 1.3 與用戶協作

```
當用戶描述需求時：
├── 需求清晰 → 直接進行 Example Mapping
├── 需求模糊 → 用三視角提問引導
└── 全新領域 → 先搜尋背景知識，再引導探索
```

**Phase 1 產出：** Rule 列表 + Example 列表 + Question 列表

---

## Phase 2：Formulation — 撰寫 Gherkin 場景

目標：將 Phase 1 的範例轉化為結構化的 Gherkin 場景。

### 2.1 撰寫原則

- **一個 Scenario 一個行為** — 不要在一個 scenario 裡塞多個邏輯
- **使用業務語言** — 用領域詞彙，不用技術術語
- **聲明式而非命令式** — 描述「什麼」而非「怎麼做」
- **具體的範例值** — 用 `"user@example.com"` 而非 `"a valid email"`

### 2.2 Feature 結構

```gherkin
Feature: [功能名稱]
  [一句話描述功能目的]

  Rule: [規則描述]
    Example: [正面範例]
      Given [前置條件]
      When [動作]
      Then [預期結果]

    Example: [反面範例]
      Given [前置條件]
      When [動作]
      Then [預期結果]
```

### 2.3 品質檢查

撰寫完成後確認：

- [ ] 每個 Rule 對應 Phase 1 的藍色卡片
- [ ] 每個 Example 對應綠色卡片
- [ ] 場景用業務語言撰寫（非技術語言）
- [ ] 場景獨立、不依賴執行順序
- [ ] 呈現給用戶確認後再進入 Phase 3

---

## Phase 3：Automation — 自動化驗證

目標：Outside-In 開發，從外層行為測試驅動到內層實作。

### 3.1 Outside-In 開發流程

```
Scenario (驗收測試)
  └─▶ 外層測試 (Controller/API)
        └─▶ 內層測試 (Service/Domain)
              └─▶ 實作程式碼
```

**步驟：**

1. 將 Gherkin scenario 轉為可執行的驗收測試
2. 執行 — 必須失敗（紅）
3. 寫外層程式碼讓驗收測試往前推進
4. 當需要內層邏輯時，切換到 TDD 循環（搭配 `/tdd` skill）
5. 逐層完成直到 scenario 全部通過

### 3.2 與 TDD 的關係

- **BDD** 回答「要建什麼？」（Outside-In）
- **TDD** 回答「怎麼建？」（Red-Green-Refactor）
- 在 Phase 3 的內層開發中，自然切換到 TDD 循環

---

## 注意事項

- **BDD 的核心是對話，不是工具** — Gherkin 是記錄共識的格式，不是目的
- **不是所有東西都需要 scenario** — 高價值的業務行為寫 scenario，細節用 unit test
- **場景會演化** — 隨著理解加深，持續精煉場景
- **先求共識再寫碼** — Phase 1、2 完成前不要急著進 Phase 3

