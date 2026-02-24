# Hangman 猜字遊戲

## 背景

你加入了一個團隊，接手一個 Java Hangman（猜字遊戲）專案。前一位工程師留下了部分完成的程式碼，目前有 Bug、有未實作的功能。你的任務是依照 4 張工單依序完成修復與開發。

專案使用 **Gradle + Cucumber BDD** 測試框架，每張工單對應一組測試，通過即代表完成。

## 遊戲規則

- 系統選定一個秘密單字，玩家每次猜一個字母
- 猜對 → 揭示該字母在單字中的所有位置
- 猜錯 → 扣一條命（預設 6 條）
- 全部猜對 → 勝利；命數歸零 → 失敗

## 專案結構

```
src/main/java/exam/question/
├── Game.java          # 核心遊戲邏輯（⚠️ 有 Bug）
├── HintProvider.java  # 提示系統（🚧 待實作）
├── Difficulty.java    # 難度列舉（唯讀）
├── WordBank.java      # 單字庫（唯讀）
└── GameConfig.java    # 常數設定（唯讀）
```

你可以修改 `Game.java` 和 `HintProvider.java`。`Difficulty`、`WordBank`、`GameConfig` 為唯讀參考。

---

## CP1：修復遊戲邏輯 Bug

**類型：** Bug Fix
**檔案：** `Game.java`

### 問題描述

QA 回報了兩個 Bug：

**Bug #1 — 重複猜測扣命**
玩家重複猜同一個錯誤字母時，每次都會被扣命。預期行為是重複猜測應被忽略，不扣命。

**Bug #2 — 勝利條件錯誤**
`isGameWon()` 的判斷邏輯有誤，導致「命數歸零」被判為勝利，而「猜出所有字母」卻沒有被判為勝利。

### 重現步驟

```
Bug #1:
1. 建立遊戲，單字為 "hello"
2. 猜 'z'（錯誤）→ 剩 5 條命 ✓
3. 再猜 'z'（重複）→ 預期仍為 5 條命，實際變成 4 條命 ✗

Bug #2:
1. 建立遊戲，單字為 "hello"
2. 依序猜 h, e, l, o → 全部猜對
3. isGameWon() 預期回傳 true，實際回傳 false ✗
```

### 驗收條件

```bash
./gradlew test --tests "exam.question.bdd.CP1Test" --no-daemon
```

---

## CP2：實作 HintProvider 提示系統

**類型：** Feature
**檔案：** `HintProvider.java`

### 需求描述

產品希望加入提示功能來改善玩家體驗。請實作 `HintProvider.java` 中的 3 個 TODO 方法：

**1. `getLetterFrequencyHint(word, guessedLetters)`**
回傳單字中出現頻率最高的**未猜**字母。如果頻率相同，取字母序較前者。全部猜完則回傳 `null`。

> 範例：word="banana", guessed={'b'} → 'a' 出現 3 次，回傳 'a'

**2. `getPositionHint(word, guessedLetters)`**
回傳第一個未猜字母的位置（**1-based**）。全部猜完則回傳 `-1`。

> 範例：word="hello", guessed={'h'} → 第 2 個字母 'e' 未猜，回傳 2

**3. `calculateScore(wordLength, wrongGuesses, hintsUsed)`**
計算分數，公式為：`max(0, wordLength × 100 - wrongGuesses × 10 - hintsUsed × 20)`

> 範例：wordLength=5, wrongGuesses=3, hintsUsed=2 → max(0, 500-30-40) = 430

### 驗收條件

```bash
./gradlew test --tests "exam.question.bdd.CP2Test" --no-daemon
```

---

## CP3：加入難度系統

**類型：** Feature
**檔案：** `Game.java`

### 需求描述

產品需要支援不同難度等級。`Difficulty` 列舉和 `WordBank` 已經準備好了，請在 `Game.java` 中完成整合：

**實作 `Game(String word, Difficulty difficulty)` 建構子**

目前這個建構子會拋出 `UnsupportedOperationException`。請實作它，根據 `difficulty.getMaxLives()` 設定命數：

| 難度 | 命數 |
|------|------|
| EASY | 8 |
| MEDIUM | 6 |
| HARD | 4 |

實作後，遊戲邏輯應與原建構子完全相同，僅命數不同。

### 驗收條件

```bash
./gradlew test --tests "exam.question.bdd.CP3Test" --no-daemon
```

---

## CP4：輸入驗證與邊界情況

**類型：** Hardening
**檔案：** `Game.java`、`HintProvider.java`

### 需求描述

Code review 發現目前程式碼缺乏防禦性處理。請加入以下驗證：

**Game.java:**
- `new Game(null)` 或 `new Game("")` 或 `new Game("   ")` → 拋出 `IllegalArgumentException`
- 遊戲結束（勝利或失敗）後呼叫 `processGuess()` → 拋出 `IllegalStateException`
- 猜測非字母字元（數字、特殊符號）→ 拋出 `IllegalArgumentException`

**HintProvider.java:**
- `getLetterFrequencyHint(null, ...)` → 回傳 `null`
- `getLetterFrequencyHint("", ...)` → 回傳 `null`
- `getPositionHint(null, ...)` → 回傳 `-1`
- `getPositionHint("", ...)` → 回傳 `-1`

### 驗收條件

```bash
./gradlew test --tests "exam.question.bdd.CP4Test" --no-daemon
```

---

## 執行所有測試

```bash
./gradlew test --no-daemon
```
