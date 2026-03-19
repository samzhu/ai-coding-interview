Feature: CP2 - 實作 HintProvider
  實作 HintProvider.java 中的三個方法。

  # --- 字母頻率提示 ---

  Scenario: 字母頻率提示：回傳出現最多次的未猜字母
    Given a hint provider
    And the word is "banana"
    And the guessed letters are "b"
    When I request a letter frequency hint
    Then the hint letter should be "a"

  Scenario: 字母頻率提示：頻率相同時依字母序排列
    Given a hint provider
    And the word is "abcd"
    And no letters have been guessed
    When I request a letter frequency hint
    Then the hint letter should be "a"

  Scenario: 字母頻率提示：全部猜完時回傳 null
    Given a hint provider
    And the word is "banana"
    And the guessed letters are "ban"
    When I request a letter frequency hint
    Then the hint letter should be null

  Scenario: 字母頻率提示：部分字母已猜過
    Given a hint provider
    And the word is "hello"
    And the guessed letters are "he"
    When I request a letter frequency hint
    Then the hint letter should be "l"

  # --- 位置提示 ---

  Scenario: 位置提示：回傳第一個未猜字母的位置
    Given a hint provider
    And the word is "hello"
    And the guessed letters are "h"
    When I request a position hint
    Then the position should be 2

  Scenario: 位置提示：第一個字母未猜時回傳 1
    Given a hint provider
    And the word is "hello"
    And no letters have been guessed
    When I request a position hint
    Then the position should be 1

  Scenario: 位置提示：全部猜完時回傳 -1
    Given a hint provider
    And the word is "hi"
    And the guessed letters are "hi"
    When I request a position hint
    Then the position should be -1

  Scenario: 位置提示：跳過已猜的位置
    Given a hint provider
    And the word is "apple"
    And the guessed letters are "ap"
    When I request a position hint
    Then the position should be 3

  # --- 分數計算 ---

  Scenario: 完美分數：無錯誤猜測且無使用提示
    Given a hint provider
    When I calculate score for word length 5 with 0 wrong guesses and 0 hints
    Then the score should be 500

  Scenario: 有錯誤猜測的分數
    Given a hint provider
    When I calculate score for word length 5 with 3 wrong guesses and 0 hints
    Then the score should be 470

  Scenario: 有使用提示的分數
    Given a hint provider
    When I calculate score for word length 5 with 0 wrong guesses and 2 hints
    Then the score should be 460

  Scenario: 同時有錯誤猜測和提示的分數
    Given a hint provider
    When I calculate score for word length 5 with 3 wrong guesses and 2 hints
    Then the score should be 430

  Scenario: 分數不會為負數
    Given a hint provider
    When I calculate score for word length 1 with 10 wrong guesses and 10 hints
    Then the score should be 0
