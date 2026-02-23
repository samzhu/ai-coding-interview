package com.interview.interview.bdd;

import com.interview.interview.application.CheckpointProgressService.CheckpointView;
import com.interview.interview.application.CreateInterviewCommand;
import com.interview.interview.application.InterviewService;
import com.interview.interview.application.SubmitCodeCommand;
import com.interview.interview.domain.Interview;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckpointProgressStepDefinitions {

    private static final UUID TWO_SUM_QUESTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FIZZBUZZ_QUESTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222221");
    private static final UUID TWO_SUM_CHECKPOINT_1_ID = UUID.fromString("11111111-1111-1111-1111-111111111112");
    private static final UUID FIZZBUZZ_CHECKPOINT_1_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private com.interview.interview.application.CheckpointProgressService checkpointProgressService;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private SharedInterviewState sharedState;

    @Given("an in-progress interview with the Two Sum question")
    public void anInProgressInterviewWithTheTwoSumQuestion() {
        var command = new CreateInterviewCommand(
                UUID.randomUUID(), UUID.randomUUID(),
                "Two Sum BDD Test", Instant.now().plusSeconds(3600), TWO_SUM_QUESTION_ID, null, null);
        Interview interview = interviewService.createInterview(command);
        interview = interviewService.startInterview(interview.getId());
        sharedState.setCurrentInterview(interview);
    }

    @Given("an in-progress interview with the FizzBuzz question")
    public void anInProgressInterviewWithTheFizzBuzzQuestion() {
        var command = new CreateInterviewCommand(
                UUID.randomUUID(), UUID.randomUUID(),
                "FizzBuzz BDD Test", Instant.now().plusSeconds(3600), FIZZBUZZ_QUESTION_ID, null, null);
        Interview interview = interviewService.createInterview(command);
        interview = interviewService.startInterview(interview.getId());
        sharedState.setCurrentInterview(interview);
    }

    @When("I request the current checkpoint")
    public void iRequestTheCurrentCheckpoint() {
        CheckpointView view = checkpointProgressService.getCurrentCheckpoint(
                sharedState.getCurrentInterview().getId());
        sharedState.setCurrentCheckpointView(view);
    }

    @Then("the checkpoint title should be {string}")
    public void theCheckpointTitleShouldBe(String expectedTitle) {
        assertThat(sharedState.getCurrentCheckpointView().title()).isEqualTo(expectedTitle);
    }

    @When("I submit correct Two Sum code for checkpoint 1")
    public void iSubmitCorrectTwoSumCodeForCheckpoint1() {
        String correctCode = """
                import java.util.*;
                public class Solution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int n = sc.nextInt();
                        int[] nums = new int[n];
                        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();
                        int target = sc.nextInt();
                        Map<Integer, Integer> map = new HashMap<>();
                        for (int i = 0; i < n; i++) {
                            int complement = target - nums[i];
                            if (map.containsKey(complement)) {
                                System.out.println(map.get(complement) + " " + i);
                                return;
                            }
                            map.put(nums[i], i);
                        }
                    }
                }
                """;
        var command = SubmitCodeCommand.singleFile(
                sharedState.getCurrentInterview().getId(),
                TWO_SUM_CHECKPOINT_1_ID,
                correctCode);
        CheckpointView view = checkpointProgressService.submitCode(command);
        sharedState.setCurrentCheckpointView(view);
    }

    @When("I submit incorrect code for checkpoint 1")
    public void iSubmitIncorrectCodeForCheckpoint1() {
        String incorrectCode = """
                public class Solution {
                    public static void main(String[] args) {
                        System.out.println("wrong answer");
                    }
                }
                """;
        var command = SubmitCodeCommand.singleFile(
                sharedState.getCurrentInterview().getId(),
                TWO_SUM_CHECKPOINT_1_ID,
                incorrectCode);
        CheckpointView view = checkpointProgressService.submitCode(command);
        sharedState.setCurrentCheckpointView(view);
    }

    @When("I submit correct FizzBuzz code for the only checkpoint")
    public void iSubmitCorrectFizzBuzzCodeForTheOnlyCheckpoint() {
        String correctCode = """
                n = int(input())
                for i in range(1, n + 1):
                    if i % 15 == 0:
                        print('FizzBuzz')
                    elif i % 3 == 0:
                        print('Fizz')
                    elif i % 5 == 0:
                        print('Buzz')
                    else:
                        print(i)
                """;
        var command = SubmitCodeCommand.singleFile(
                sharedState.getCurrentInterview().getId(),
                FIZZBUZZ_CHECKPOINT_1_ID,
                correctCode);
        CheckpointView view = checkpointProgressService.submitCode(command);
        sharedState.setCurrentCheckpointView(view);
    }

    @Then("the interview should be automatically completed")
    public void theInterviewShouldBeAutomaticallyCompleted() {
        Interview interview = interviewRepository
                .findById(sharedState.getCurrentInterview().getId())
                .orElseThrow();
        assertThat(interview.getStatus()).isEqualTo("COMPLETED");
    }
}
