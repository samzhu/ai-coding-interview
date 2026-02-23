package com.interview.interview.bdd;

import com.interview.execution.CodeExecutionRequest;
import com.interview.execution.CodeExecutionResult;
import com.interview.execution.CodeExecutionService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class CodeExecutionStepDefinitions {

    @Autowired
    private CodeExecutionService codeExecutionService;

    private String currentCode;
    private String currentLanguage;
    private CodeExecutionResult currentResult;

    @Given("a Java code submission that prints FizzBuzz for N=5")
    public void aJavaCodeSubmissionThatPrintsFizzBuzzForN5() {
        currentLanguage = "java";
        currentCode = """
                public class Solution {
                    public static void main(String[] args) {
                        int n = 5;
                        for (int i = 1; i <= n; i++) {
                            if (i % 15 == 0) {
                                System.out.println("FizzBuzz");
                            } else if (i % 3 == 0) {
                                System.out.println("Fizz");
                            } else if (i % 5 == 0) {
                                System.out.println("Buzz");
                            } else {
                                System.out.println(i);
                            }
                        }
                    }
                }
                """;
    }

    @When("the Java code is executed")
    public void theJavaCodeIsExecuted() {
        currentResult = codeExecutionService.execute(
                new CodeExecutionRequest(currentLanguage, currentCode, "", 60));
    }

    @Then("the execution status should be {string}")
    public void theExecutionStatusShouldBe(String expectedStatus) {
        assertThat(currentResult.status().name()).isEqualTo(expectedStatus);
    }

    @And("the stdout should contain {string}")
    public void theStdoutShouldContain(String expected) {
        assertThat(currentResult.stdout()).contains(expected);
    }

    @Given("a Java code submission with an infinite loop")
    public void aJavaCodeSubmissionWithAnInfiniteLoop() {
        currentLanguage = "java";
        currentCode = """
                public class Solution {
                    public static void main(String[] args) {
                        while (true) {}
                    }
                }
                """;
    }

    @When("the code is executed with timeout {int} seconds")
    public void theCodeIsExecutedWithTimeoutSeconds(int timeoutSeconds) {
        currentResult = codeExecutionService.execute(
                new CodeExecutionRequest(currentLanguage, currentCode, "", timeoutSeconds));
    }
}
