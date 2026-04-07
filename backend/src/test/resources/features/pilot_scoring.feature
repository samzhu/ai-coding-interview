Feature: Pilot Score is computed when an interview completes

  Scenario: Zero-interaction interview is scored as PASSENGER without LLM call
    Given a completed interview with no AI conversations and no checkpoints
    When the InterviewCompletedEvent is published
    Then within 30 seconds an InterviewScore row exists for that interview
    And the pilot_verdict is "PASSENGER"
    And the outcome_score is 0.0
    And the judge_error_reason is null
