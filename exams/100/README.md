# Hangman 猜字遊戲

## 背景

你加入了一個小團隊，接手前一位工程師留下的 Java Hangman 專案。
程式碼已有基本框架，但存在已知問題，也有尚未完成的功能。
你的任務是依照平台上的工單（Checkpoint），逐步完成修復與開發。

專案使用 **Gradle + Cucumber BDD** 測試框架，每張工單對應一組測試，通過即代表完成。

## 遊戲規則

- 系統選定一個秘密單字，玩家每次猜一個字母
- 猜對 → 揭示該字母在單字中的所有位置
- 猜錯 → 扣一條命（預設 6 條）
- 全部猜對 → 勝利；命數歸零 → 失敗

## 專案結構

```
src/main/java/exam/question/
├── Game.java          # 核心遊戲邏輯（可修改）
├── HintProvider.java  # 提示系統（可修改）
├── Difficulty.java    # 難度列舉（唯讀）
├── WordBank.java      # 單字庫（唯讀）
└── GameConfig.java    # 常數設定（唯讀）
```

`Game.java` 和 `HintProvider.java` 是你的主要工作區域。
`Difficulty`、`WordBank`、`GameConfig` 為參考用唯讀檔案。

## 快速開始

### 體驗遊戲

在開始寫程式之前，可以先實際玩玩看，感受遊戲的行為：

```bash
./gradlew run --no-daemon
```

> 提示：試著輸入 `hint` 指令，觀察提示功能目前的狀態。

### 執行測試

平台會根據你完成的工單自動判定通過。你也可以在本地手動執行：

```bash
# 執行全部測試
./gradlew test --no-daemon

# 執行特定工單的測試（以工單編號為準）
./gradlew test --tests "exam.question.bdd.CP1Test" --no-daemon
./gradlew test --tests "exam.question.bdd.CP2Test" --no-daemon
./gradlew test --tests "exam.question.bdd.CP3Test" --tests "exam.question.bdd.CP4Test" --no-daemon
```

## 如何進行

1. 閱讀右側面板的工單描述，了解當前任務
2. 閱讀程式碼、理解問題所在
3. 修改程式碼完成修復或開發
4. 執行測試驗證結果
5. 通過後繼續下一張工單

> 善用右側的 **AI 助手**——他可以解釋 Java 概念、Gradle 指令、測試框架用法，
> 以及協助你理解程式碼行為，但不會直接給出工單的答案。
