Feature: CP4 - 邊界情況
  加入適當的輸入驗證與錯誤處理。

  # --- 建構遊戲的驗證 ---

  Scenario: 以 null 建立遊戲應拋出例外
    When I create a game with null word
    Then an IllegalArgumentException should be thrown

  Scenario: 以空字串建立遊戲應拋出例外
    When I create a game with empty word ""
    Then an IllegalArgumentException should be thrown

  Scenario: 以空白字串建立遊戲應拋出例外
    When I create a game with empty word "   "
    Then an IllegalArgumentException should be thrown

  # --- 遊戲結束後繼續猜測 ---

  Scenario: 遊戲獲勝後繼續猜測應拋出例外
    Given a new game with word "ab"
    When the player guesses "a"
    And the player guesses "b"
    Then the game should be won
    When I attempt to guess "c" on a finished game
    Then an IllegalStateException should be thrown

  Scenario: 遊戲落敗後繼續猜測應拋出例外
    Given a new game with word "abcdefg"
    When the player guesses "x"
    And the player guesses "y"
    And the player guesses "z"
    And the player guesses "w"
    And the player guesses "v"
    And the player guesses "u"
    Then the game should be lost
    When I attempt to guess "t" on a finished game
    Then an IllegalStateException should be thrown

  # --- 非字母字元猜測 ---

  Scenario: 猜測數字應拋出例外
    Given a new game with word "hello"
    When I attempt to guess "5" with validation
    Then an IllegalArgumentException should be thrown

  Scenario: 猜測特殊字元應拋出例外
    Given a new game with word "hello"
    When I attempt to guess "@" with validation
    Then an IllegalArgumentException should be thrown

  # --- HintProvider 邊界情況 ---

  Scenario: 字母頻率提示：null 單字回傳 null
    Given a hint provider
    When I request a letter frequency hint for null word
    Then the hint letter should be null

  Scenario: 字母頻率提示：空字串回傳 null
    Given a hint provider
    And the word is ""
    And no letters have been guessed
    When I request a letter frequency hint
    Then the hint letter should be null

  Scenario: 位置提示：null 單字回傳 -1
    Given a hint provider
    When I request a position hint for null word
    Then the position should be -1

  Scenario: 位置提示：空字串回傳 -1
    Given a hint provider
    And the word is ""
    And no letters have been guessed
    When I request a position hint
    Then the position should be -1
