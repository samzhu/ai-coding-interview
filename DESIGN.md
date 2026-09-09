# DESIGN.md — AI 程式面試平台

## 1. 視覺主題與氛圍

平台是一個 **專注、專業的程式工作區**，用於高壓的技術面試場景。候選人會在這裡承受 60 分鐘的壓力；面試官則需要連續看好幾個小時。每一個視覺決策都圍繞著 **平靜的專注、長時間閱讀與「程式碼優先」的層級** 來設計。

- **氛圍**：嚴肅、技術導向、中性。沒有行銷式的誇飾，沒有玩樂感的色彩，沒有裝飾性的漸層。
- **參考美學**：**VS Code Dark+** —— 同樣的炭灰色面、同樣的 VS Code Blue 強調色、同樣的克制感。一位資深工程師在第一秒就應該感到「自在」。
- **dark-only 是設計選擇**：沒有亮色主題，也沒有主題切換器。程式面試發生在被刻意調暗的螢幕上，亮色主題會非常刺眼。`shared` 套件雖然有提供 light/dark 雙主題的 token，但 **兩個 app 都把它整個覆寫成 dark-only**。
- **程式碼是主角**：所有 UI 外殼（sidebar、tabs、面板）都退回到低彩度的灰階，讓螢幕上最亮的東西是原始碼、測試輸出和 AI 回應。
- **以克制建立信任**：只有一個強調色。狀態的意義由形狀、位置與單一語意色（destructive 紅）承載 —— 永遠不靠裝飾性的色票來表達。

**反向氛圍**：沒有霓虹、沒有 glassmorphism、卡片沒有柔和陰影、沒有「萬物皆圓」、沒有粉嫩的數據可視化。

---

## 2. 色票與角色

所有顏色都以 **OKLCH** 撰寫（感知均勻色彩空間）。Hex 值僅為近似參考。Token 命名沿用 **shadcn/ui** 的語意慣例，因此任何 shadcn 元件都能直接放入而不需修改。

### 2.1 表面層級（VS Code Dark+）

| Token                   | OKLCH                          | Hex ≈     | 角色                                              |
| ----------------------- | ------------------------------ | --------- | ------------------------------------------------- |
| `--background`          | `oklch(0.205 0.003 264)`       | `#1e1e1e` | App 背景，最深的表面                              |
| `--foreground`          | `oklch(0.838 0.000 0)`         | `#cccccc` | 主要文字。在 background 上 **10.38:1 AAA**       |
| `--card`                | `oklch(0.228 0.003 264)`       | `#252526` | 卡片、sidebar、抬升面板                           |
| `--card-foreground`     | `oklch(0.838 0.000 0)`         | `#cccccc` | 卡片上的文字。**8.58:1 AAA**                      |
| `--popover`             | `oklch(0.257 0.003 264)`       | `#2d2d2d` | Popover、下拉選單、Dialog（再高一階）            |
| `--popover-foreground`  | `oklch(0.838 0.000 0)`         | `#cccccc` | Popover 上的文字                                  |
| `--sidebar`             | `oklch(0.228 0.003 264)`       | `#252526` | Sidebar / 導覽列背景（與 card 相同）              |
| `--sidebar-foreground`  | `oklch(0.838 0.000 0)`         | `#cccccc` | Sidebar 文字                                      |

**表面堆疊規則**：`background (1e1e1e) → card (252526) → popover (2d2d2d)`。每一層抬升只 +0.02 OKLCH lightness —— 剛好夠分離，不需要靠陰影。

### 2.2 品牌強調 —— VS Code Blue

| Token                  | OKLCH                       | Hex ≈     | 角色                                                   |
| ---------------------- | --------------------------- | --------- | ------------------------------------------------------ |
| `--primary`            | `oklch(0.537 0.162 243)`    | `#007acc` | **唯一**的品牌色。按鈕、連結、active tab               |
| `--primary-foreground` | `oklch(0.985 0.000 0)`      | `#ffffff` | Primary 填色上的文字                                   |
| `--ring`               | `oklch(0.537 0.162 243)`    | `#007acc` | Focus ring —— 同樣的藍，刻意醒目                       |
| `--sidebar-primary`    | `oklch(0.537 0.162 243)`    | `#007acc` | 啟用中的導覽項目                                       |

> **為什麼只有一個品牌色？** 加入第二個顏色就會強迫使用者去學「它代表什麼」。VS Code 也是如此 —— 藍色是整個 IDE 中唯一的「決策色」。

### 2.3 Secondary、muted、accent（低強調表面）

| Token                  | OKLCH                       | Hex ≈     | 角色                                                       |
| ---------------------- | --------------------------- | --------- | ---------------------------------------------------------- |
| `--secondary`          | `oklch(0.257 0.003 264)`    | `#2d2d2d` | 次要按鈕、未啟用 tab 的背景                                |
| `--muted`              | `oklch(0.248 0.005 220)`    | `#2a2d2e` | Muted 背景（細微 row hover、code block 背景）              |
| `--muted-foreground`   | `oklch(0.650 0.000 0)`      | `≈#999999`| 輔助文字 / placeholder。在 card 上 **5.85:1 AA**           |
| `--accent`             | `oklch(0.257 0.003 264)`    | `#2d2d2d` | 選單項目的 hover 狀態                                      |
| `--accent-foreground`  | `oklch(0.838 0.000 0)`      | `#cccccc` | accent 上的文字                                            |

> **`muted-foreground` 被刻意調亮** —— 從原本常見的 `#858585` 提升到 `#999999`。原本的數值在卡片上對比是 4.21:1，未通過 AA。CSS 內已有註解標註，**請不要「修回去」**。

### 2.4 Border 與輸入框

| Token       | OKLCH                       | Hex ≈     | 角色                                                       |
| ----------- | --------------------------- | --------- | ---------------------------------------------------------- |
| `--border`  | `oklch(0.310 0.000 0)`      | `≈#474747`| 所有邊框、分隔線                                           |
| `--input`   | `oklch(0.420 0.000 0)`      | `≈#5a5a5a`| 表單輸入框邊框。**比 `--border` 更亮**，以維持卡片上對比（≥3:1） |

> **為什麼 `--input` 比 `--border` 亮？** 原本 shadcn 預設兩者用同一個值，但在 `#252526` 的卡片上對比只有 1.57:1，遠低於 WCAG 1.4.11 對非文字元素的最低要求（3:1）。我們把 input 邊框拉到 `≈#5a5a5a`，讓表單欄位在卡片上仍清晰可見。

### 2.5 語意色 —— 只有 destructive

| Token           | OKLCH                          | Hex ≈     | 角色                                              |
| --------------- | ------------------------------ | --------- | ------------------------------------------------- |
| `--destructive` | `oklch(0.704 0.191 22.216)`    | `≈#f87171`| 錯誤、失敗的測試結果、取消動作。**6.03:1 AA**     |

> 刻意 **沒有** `--success`、`--warning`、`--info` 這類 token。狀態的意義由測試結果面板、Badge 與文字本身傳達。如果你發現自己想要一個綠色的勾勾顏色 —— 請改用下方的 chart 色票。

### 2.6 數據可視化 —— Chart 色票

| Token       | OKLCH                       | 約略色相      | 建議用途                              |
| ----------- | --------------------------- | ------------- | ------------------------------------- |
| `--chart-1` | `oklch(0.537 0.162 243)`    | VS Code 藍    | 主要序列（與品牌色一致）              |
| `--chart-2` | `oklch(0.750 0.115 175)`    | 藍綠          | 次要序列 / 「通過」                   |
| `--chart-3` | `oklch(0.700 0.170 60)`     | 橘            | 第三序列 / 「警告」                   |
| `--chart-4` | `oklch(0.627 0.265 303)`    | 紫            | 第四序列                              |
| `--chart-5` | `oklch(0.645 0.246 16)`     | 粉紅紅        | 第五序列 / 「失敗」                   |

任何多序列的視覺化（雷達圖、長條圖、評分拆解）都用 `chart-*`。永遠從 `chart-1` 開始，**依序使用，不要跳號**。

---

## 3. 字體規則

| Token         | 值                                       |
| ------------- | ---------------------------------------- |
| `--font-sans` | `var(--font-geist-sans)` —— **Geist Sans** |
| `--font-mono` | `var(--font-geist-mono)` —— **Geist Mono** |

- **所有 UI 都用 Geist Sans。** Geist 中性、略帶幾何感的字形與 VS Code 美學一致。
- **所有程式碼、檔案路徑、終端機輸出與測試結果都用 Geist Mono。** 不要直接寫 `font-family: monospace` —— 一律走 `font-mono`。
- **數字使用等寬數字**（分數、計時器、checkpoint 計數等）：`font-variant-numeric: tabular-nums`。

### 層級（Tailwind utility）

| 角色             | Class                              | 備註                                |
| ---------------- | ---------------------------------- | ----------------------------------- |
| 頁面標題         | `text-3xl font-semibold`           | 每頁只用一次                        |
| 區塊標題         | `text-xl font-semibold`            | 卡片標題                            |
| 子區塊           | `text-base font-medium`            |                                     |
| 內文             | `text-sm`（表單 / UI 預設）        | 密集 workspace UI，**非** `text-base` |
| 輔助 / caption   | `text-xs text-muted-foreground`    |                                     |
| 行內程式碼       | `font-mono text-[0.9em]`           | 比周圍文字略小                      |

> 預設內文是 `text-sm`（14px），不是 `text-base`（16px）。這是一個 workspace 產品 —— 資訊密度比長文閱讀的舒適性更重要。

---

## 4. 元件樣式

所有元件都建立在 **shadcn/ui** 之上。**不要 fork**，而是透過 token 重新樣式化。

### 按鈕

| Variant       | 背景                       | 文字                          | 何時使用                                       |
| ------------- | -------------------------- | ----------------------------- | ---------------------------------------------- |
| `default`     | `bg-primary`               | `text-primary-foreground`     | Submit、Run、Create —— 每頁 **唯一** 主要 CTA  |
| `secondary`   | `bg-secondary`             | `text-secondary-foreground`   | 次要動作、「複製連結」                         |
| `outline`     | `border` + 透明背景        | `text-foreground`             | 取消、次要導覽                                 |
| `ghost`       | 透明                       | `text-foreground`             | 工具列圖示按鈕、tab 關閉                       |
| `destructive` | `bg-destructive`           | 白色                          | 取消面試、刪除                                 |

高度：`h-9`（預設）、`h-10`（與表單欄位對齊）、`h-8`（工具列）。一律 `--radius-md`。

### 卡片

```
bg-card / text-card-foreground
border border-border
rounded-[var(--radius-lg)]   /* 0.625rem */
```

**沒有陰影。** 抬升靠 +0.02 lightness，不靠 `box-shadow`。加陰影會破壞 VS Code 的氛圍。

### 輸入框

- 背景：`bg-background`（沒錯 —— 即使在卡片內，input 也要往**下**降一階，看起來像「鑲嵌進去」）
- 邊框：`border-input`（較亮的 `#5a5a5a` token）
- Focus：用 `--ring`（VS Code Blue），2px outline offset

### Tabs（編輯器中的檔案 tab）

- 未啟用：`bg-secondary` + `text-muted-foreground`
- 啟用中：`bg-background` + `text-foreground` + `border-b-2 border-primary`
- 這完全模仿 VS Code 編輯器的 tab 條，**請不要改**。

### Badge（面試狀態）

| 狀態          | Variant       | 顏色             |
| ------------- | ------------- | ---------------- |
| `SCHEDULED`   | outline       | muted 文字       |
| `ACTIVE`      | default       | 主要藍           |
| `COMPLETED`   | secondary     | secondary 填色   |
| `CANCELLED`   | destructive   | 紅               |

### 程式碼編輯器（CodeMirror 6）

- 主題：**VS Code Dark+**（已與 app 其他部分對齊）
- 字體：Geist Mono，13px
- 行號：`text-muted-foreground`
- 選取：VS Code Blue，約 30% 透明度
- **絕對不要把 CodeMirror 包在加了內距的卡片裡** —— 讓它直接貼到面板邊緣，就像真實的 VS Code 一樣。

### 終端機（xterm.js，候選人 app）

- 背景：對齊 `--background`（`#1e1e1e`）
- 前景：`#cccccc`
- 游標：VS Code Blue，閃爍
- 選取：與 CodeMirror 相同

### AI 對話面板

- 建立在 AI Elements + AI SDK v6 上。
- 訊息背景：assistant 用 `bg-muted`，user 用 `bg-primary/10`。
- 串流 markdown 由 `streamdown` 渲染。**候選人 app 中已隱藏 code-block 的 download/copy 按鈕**（面試完整性 —— 不要還原）。

---

## 5. 版面原則

### 5.1 間距 scale

使用 Tailwind 預設 scale（`0`、`0.5`、`1`、`1.5`、`2`、`3`、`4`、`6`、`8`、`12`、`16`）。版面以 **4 的倍數**（`4`、`8`、`12`、`16`）為主，較小的步階用於元件內部緊湊處。

| 使用情境              | 間距                    |
| --------------------- | ----------------------- |
| 頁面外圍 padding      | 桌面 `p-6`，手機 `p-4`  |
| 卡片內 padding        | `p-6`                   |
| 區塊垂直節奏          | `space-y-6`             |
| 表單欄位間距          | `space-y-4`             |
| 圖示與文字並排        | `gap-2`                 |
| 工具列項目間距        | `gap-1.5`               |

### 5.2 Grid 與工作區版面

面試工作區是一個 **可調整大小的三欄版面**：

```
┌───────────┬─────────────────────────┬──────────────┐
│ FileTree  │   CodeMirror editor     │  AI 對話 /   │
│  (可摺疊) │                         │  測試結果    │
│           │                         │  Tabs        │
│           ├─────────────────────────┤              │
│           │  Terminal / xterm.js    │              │
└───────────┴─────────────────────────┴──────────────┘
```

- 使用 `react-resizable-panels`（已內建在 shadcn/ui 中）。
- 預設比例：檔案樹 18% / 編輯器 52% / 右側面板 30%。
- 底部終端機未使用時，會收合成 32px 的標題列。

### 5.3 留白哲學

這是一個 **密集的 workspace**，不是行銷頁。留白的目的是分隔區域，**不是裝飾**。如果一個畫面看起來「很空靈」，那你大概是浪費了候選人寫程式需要的螢幕空間。

---

## 6. 深度與抬升

**這個產品幾乎沒有任何陰影。** 請再讀一次。

- **卡片**：沒有陰影。抬升靠 +0.02 OKLCH lightness 步階。
- **Popover / 下拉選單**：允許單一柔和陰影 `shadow-md`，因為它們會浮在不可預期的內容之上。
- **Dialog**：背景遮罩 `bg-background/80 backdrop-blur-sm`，dialog 本身用 `shadow-lg`。**這是整個專案中唯一可以用 `shadow-lg` 的地方**。

抬升層級（用 lightness，不用 shadow）：

```
0  background    #1e1e1e   （編輯器畫布）
1  card/sidebar  #252526   （面板、卡片）
2  popover       #2d2d2d   （選單、hover 表面）
3  dialog        #2d2d2d + shadow-lg + backdrop-blur
```

---

## 7. 圓角

基準圓角：`--radius: 0.625rem`（10px）。完整 scale 由它計算而來：

| Token         | 值                              | 用途                              |
| ------------- | ------------------------------- | --------------------------------- |
| `--radius-sm` | `calc(--radius - 4px)` = 6px    | Input、Badge、小按鈕              |
| `--radius-md` | `calc(--radius - 2px)` = 8px    | 按鈕、Tabs                        |
| `--radius-lg` | `--radius` = 10px               | 卡片、Popover                     |
| `--radius-xl` | `calc(--radius + 4px)` = 14px   | Dialog、大型功能卡                |
| `--radius-2xl` ~ `--radius-4xl` | 最大 26px        | 保留 —— 目前未使用                |

> **除了頭像和走過畫面的乳牛動畫，永遠不要用 `rounded-full`。** 膠囊型按鈕在 VS Code 美學中會很違和。

---

## 8. Do's and Don'ts

### ✅ 應該

- **使用語意 token。** `bg-card`，永遠不要 `bg-[#252526]`。
- **輔助文字優先用 `text-muted-foreground`** —— 這是唯一一個我們已經驗證過 AA 對比的「次要色」。
- **每頁只有一個 CTA。** 如果你發現需要兩個主要按鈕，那你應該需要兩頁。
- **不確定時就照 VS Code 慣例做** —— 檔案樹、tabs、status bar、command palette。
- **改顏色時請在 CSS 註解標註 WCAG 對比率**，方式與既有 `globals.css` 一致。
- **只在 dark mode 中測試** —— 沒有 light mode 可以壞掉。

### 🚫 不該

- **不要引入第二個品牌色。** 沒有紫色「AI」漸層、沒有綠色「成功」按鈕。**只有一個藍色**。
- **不要在卡片上加 `box-shadow`。** 用 lightness 步階。
- **不要用 Tailwind 預設的 `gray-*` / `slate-*` 顏色。** 它們不在我們的 token 系統內，放在 OKLCH 表面上會微妙地不協調。
- **表單 / workspace UI 不要用 `text-base`** —— 這裡的內文預設是 `text-sm`。
- **不要還原候選人 app 中 streamdown 的 copy/download 按鈕**（刻意隱藏 —— 面試完整性）。
- **不要在 app 的 `globals.css` 中用 `@import` 引入 `frontend/packages/shared/globals.css`。** PostCSS 路徑解析會壞掉，每個 app 必須內嵌完整 token block。（已在 `MEMORY.md` 記錄）
- **不要在這個 app 裡放行銷頁。** 如果需要 landing page，請開另一個獨立網站。

---

## 9. 響應式行為

平台是 **桌面優先**。手機上做程式面試不是真實使用情境 —— 但 admin 頁面至少要在平板上能用。

### 斷點（Tailwind 預設）

| 斷點   | 最小寬度  | 行為                                              |
| ------ | --------- | ------------------------------------------------- |
| `sm`   | 640px     | Admin 表單變單欄                                  |
| `md`   | 768px     | Admin 表格收合成卡片清單                          |
| `lg`   | 1024px    | **候選人工作區的最低支援寬度** —— 低於此顯示「請使用桌面」的提示 |
| `xl`   | 1280px    | 預設版面                                          |
| `2xl`  | 1536px    | Workspace 擴展使用額外空間                        |

### 觸控目標

即使是桌面優先，所有可互動元素至少 **40×40 px**，讓 admin app 在平板或觸控螢幕上也能操作。

### 收合規則

- 檔案樹 → 在 `lg` 以下收合為 40px 圖示軌道。
- AI 對話面板 → 在 `xl` 以下變成 slide-over 抽屜。
- 終端機 → 在 `lg` 以下變成全螢幕覆蓋。

---

## 10. Agent Prompt Guide

當你要請 AI 代理為這個專案產生新 UI 時，把下面這個區塊貼進 prompt。它用最緊湊的形式描述了整個設計語言。

### 快速參考卡

```
主題：    VS Code Dark+ 啟發、dark-only、無主題切換
品牌：    一個顏色 —— VS Code Blue oklch(0.537 0.162 243) ≈ #007acc
表面：    bg #1e1e1e → card #252526 → popover #2d2d2d（只用 lightness，不用陰影）
文字：    Geist Sans，內文 = text-sm。程式碼 = Geist Mono。
前景：    #cccccc（在 bg 上 AAA）。Muted = #999999（在 card 上 AA）。
邊框：    #474747。Input：#5a5a5a（刻意調亮，WCAG 修正）。
圓角：    base 10px，sm 6 / md 8 / lg 10 / xl 14
元件：    僅 shadcn/ui —— 透過 token 重新樣式，不要 fork。
版面：    密集 workspace，p-6 卡片，沒有裝飾性留白。
反向：    沒有第二個品牌色、沒有卡片陰影、沒有霓虹、沒有漸層、
          沒有 rounded-full 按鈕、沒有 Tailwind gray-*/slate-* 工具類別。
```

### Drop-in agent prompt（直接貼上即可）

> 你正在為 AI Coding Interview Platform 建構 UI。產品是一個嚴肅、dark-only、VS Code Dark+ 風格的程式工作區。只能使用 shadcn/ui 元件與綁定到專案 CSS 變數的 Tailwind utility（`bg-background`、`bg-card`、`text-foreground`、`text-muted-foreground`、`bg-primary`、`border-border` 等）。唯一的品牌色是 VS Code Blue（`--primary`）；不要引入第二種顏色。卡片沒有陰影 —— 抬升是 +0.02 OKLCH lightness 步階。內文預設 `text-sm`。每頁恰好一個主要 CTA。不確定時遵循 VS Code 慣例（檔案樹、tabs、status bar）。不要使用 Tailwind 的 `gray-*` / `slate-*` 類別 —— 一律走 token。完整規格請讀專案根目錄的 `DESIGN.md`。

---

## 參考資料

- `frontend/apps/admin/src/app/globals.css` —— token 真相來源
- `frontend/apps/candidate/src/app/globals.css` —— 同樣的 token + xterm.css + 乳牛動畫
- `frontend/packages/shared/globals.css` —— 預設的 shadcn zinc tokens（被兩個 app 覆寫）
