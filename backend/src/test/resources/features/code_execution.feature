Feature: Code Execution
  As the interview system
  I want to execute candidate code in isolation
  So that I can verify correctness safely

  Scenario: Execute valid Java FizzBuzz code
    Given a Java code submission that prints FizzBuzz for N=5
    When the Java code is executed
    Then the execution status should be "SUCCESS"
    And the stdout should contain "Fizz"
    And the stdout should contain "Buzz"

  Scenario: Detect timeout for infinite loop
    Given a Java code submission with an infinite loop
    When the code is executed with timeout 2 seconds
    Then the execution status should be "TIMEOUT"
