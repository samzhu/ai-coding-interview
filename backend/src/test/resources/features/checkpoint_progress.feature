Feature: Checkpoint Progress
  As a candidate
  I want to submit code for each checkpoint
  So that my solution is evaluated and I can progress through the interview

  Background:
    Given the system is ready to accept interview requests

  Scenario: Get current checkpoint for an in-progress interview
    Given an in-progress interview with the Hangman question
    When I request the current checkpoint
    Then the checkpoint title should be "Bug Fix — 修復遊戲邏輯"
    And the checkpoint status should be "PENDING"

  Scenario: Submit correct code and pass a checkpoint
    Given an in-progress interview with the Hangman question
    When I submit correct code for checkpoint 1
    Then the checkpoint status should be "PASSED"

  Scenario: Submit incorrect code and fail a checkpoint
    Given an in-progress interview with the Hangman question
    When I submit incorrect code for checkpoint 1
    Then the checkpoint status should be "FAILED"

  Scenario: Complete interview when all checkpoints pass
    Given an in-progress interview with the Hangman question
    When I submit correct code for all 4 checkpoints
    Then the interview should be automatically completed
