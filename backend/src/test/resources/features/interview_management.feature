Feature: Interview Management
  As a hiring manager
  I want to manage coding interviews
  So that I can evaluate candidates effectively

  Background:
    Given the system is ready to accept interview requests

  Scenario: Successfully create a new interview
    Given a candidate with id "candidate-001"
    And an interviewer with id "interviewer-001"
    When I create an interview with title "Java Backend Engineer Interview" scheduled at "2026-03-01T10:00:00Z"
    Then the interview should be created with status "SCHEDULED"
    And the interview should have title "Java Backend Engineer Interview"

  Scenario: Fail to create interview with missing required fields
    Given a candidate with id "candidate-002"
    When I attempt to create an interview without a title
    Then the creation should fail with a validation error
    And the error message should mention "title"

  Scenario: Start a scheduled interview
    Given an existing interview with status "SCHEDULED"
    When the interviewer starts the interview
    Then the interview status should change to "IN_PROGRESS"
    And the interview started_at should be recorded

  Scenario: Cannot start an interview that is not in SCHEDULED status
    Given an existing interview with status "IN_PROGRESS"
    When the interviewer attempts to start the interview again
    Then the operation should fail with an illegal state error
    And the interview status should remain "IN_PROGRESS"

  Scenario: Complete an interview and trigger evaluation event
    Given an existing interview with status "IN_PROGRESS"
    When the interviewer completes the interview
    Then the interview status should change to "COMPLETED"
    And the interview completed_at should be recorded
    And an evaluation event should be published
