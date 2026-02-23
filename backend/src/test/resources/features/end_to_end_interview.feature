Feature: End-to-End Interview Flow
  As an HR manager and a candidate
  I want to complete a full interview cycle
  So that the candidate can demonstrate their coding skills

  Background:
    Given the system is ready to accept interview requests

  Scenario: Complete full interview lifecycle for Two Sum question
    # Step 1: HR creates interview
    Given a candidate with id "candidate-e2e-001"
    And an interviewer with id "interviewer-e2e-001"
    When I create an interview with title "Two Sum Interview E2E" scheduled at "2026-04-01T10:00:00Z"
    Then the interview should be created with status "SCHEDULED"

    # Step 2: Generate invitation token
    When the interviewer creates an invitation for the interview
    Then an invitation token should be generated

    # Step 3: Candidate joins via token
    When the candidate joins using the invitation token
    Then the interview should automatically start with status "IN_PROGRESS"
    And the first checkpoint should be available

    # Step 4: Candidate views current checkpoint
    When the candidate requests the current checkpoint
    Then the checkpoint status should be "PENDING"
    And the checkpoint should contain starter code

  Scenario: Interview starts when candidate joins via valid token
    Given an existing interview with status "SCHEDULED"
    When the interviewer creates an invitation for the interview
    And the candidate joins using the invitation token
    Then the interview status should change to "IN_PROGRESS"
