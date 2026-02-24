Feature: CP3 - 難度系統
  為遊戲加入難度等級支援。

  Scenario: 簡單難度有 8 條命
    Given a new game with word "hello" and difficulty EASY
    Then the remaining lives should be 8

  Scenario: 中等難度有 6 條命
    Given a new game with word "hello" and difficulty MEDIUM
    Then the remaining lives should be 6

  Scenario: 困難難度有 4 條命
    Given a new game with word "hello" and difficulty HARD
    Then the remaining lives should be 4

  Scenario: 含難度的遊戲仍能正常運作
    Given a new game with word "cat" and difficulty HARD
    When the player guesses "c"
    And the player guesses "a"
    And the player guesses "t"
    Then the game should be won

  Scenario: 困難模式更快落敗
    Given a new game with word "hello" and difficulty HARD
    When the player guesses "a"
    And the player guesses "b"
    And the player guesses "c"
    And the player guesses "d"
    Then the game should be lost

  Scenario: WordBank 能回傳指定難度的單字
    When I request a random word for difficulty EASY
    Then the word should not be null
