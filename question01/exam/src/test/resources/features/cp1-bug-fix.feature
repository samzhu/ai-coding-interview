Feature: CP1 - Bug 修復：遊戲邏輯
  修復 Game.java 中的 Bug，讓所有場景通過。

  Background:
    Given a new game with word "hello"

  Scenario: 猜對的字母會顯示在遮罩單字中
    When the player guesses "h"
    Then the guess result should be true
    And the masked word should be "h____"

  Scenario: 猜錯會扣一條命
    When the player guesses "z"
    Then the guess result should be false
    And the remaining lives should be 5

  Scenario: 猜對會揭示所有相符的字母位置
    When the player guesses "l"
    Then the masked word should be "__ll_"

  Scenario: 大小寫不影響猜測
    When the player guesses "H"
    Then the guess result should be true
    And the masked word should be "h____"

  Scenario: 重複猜測錯誤字母不應額外扣命
    When the player guesses "z"
    And the player guesses "z"
    Then the remaining lives should be 5

  Scenario: 重複猜測正確字母不會有副作用
    When the player guesses "h"
    And the player guesses "h"
    Then the remaining lives should be 6
    And the masked word should be "h____"

  Scenario: 猜出所有字母即獲勝
    When the player guesses "h"
    And the player guesses "e"
    And the player guesses "l"
    And the player guesses "o"
    Then the game should be won

  Scenario: 命數用完即落敗而非獲勝
    When the player guesses "a"
    And the player guesses "b"
    And the player guesses "c"
    And the player guesses "d"
    And the player guesses "f"
    And the player guesses "g"
    Then the game should be lost
    And the game should not be won
