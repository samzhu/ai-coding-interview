package com.interview.interview.bdd;

import com.interview.interview.application.CheckpointProgressService.CheckpointView;
import com.interview.interview.application.CreateInterviewCommand;
import com.interview.interview.application.InterviewService;
import com.interview.interview.application.SubmitCodeCommand;
import com.interview.interview.domain.Interview;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckpointProgressStepDefinitions {

    private static final String QUESTION_ID = "question1";
    private static final String[] CHECKPOINT_IDS = {"1", "2", "3", "4"};

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private com.interview.interview.application.CheckpointProgressService checkpointProgressService;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private SharedInterviewState sharedState;

    @Before
    public void resetExecutor() {
        TestCodeExecutorConfig.ControllableCodeExecutor.reset();
        TestCodeExecutorConfig.TestContainerManager.reset();
    }

    @Given("an in-progress interview with the Hangman question")
    public void anInProgressInterviewWithTheHangmanQuestion() {
        var command = new CreateInterviewCommand(
                UUID.randomUUID(), UUID.randomUUID(),
                "Hangman BDD Test", Instant.now().plusSeconds(3600), QUESTION_ID, null, null);
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

    @When("I submit correct code for checkpoint 1")
    public void iSubmitCorrectCodeForCheckpoint1() {
        TestCodeExecutorConfig.TestContainerManager.forcePass();
        var command = new SubmitCodeCommand(
                sharedState.getCurrentInterview().getId(),
                CHECKPOINT_IDS[0]);
        CheckpointView view = checkpointProgressService.submitCode(command);
        sharedState.setCurrentCheckpointView(view);
    }

    @When("I submit incorrect code for checkpoint 1")
    public void iSubmitIncorrectCodeForCheckpoint1() {
        TestCodeExecutorConfig.TestContainerManager.forceFail();
        var command = new SubmitCodeCommand(
                sharedState.getCurrentInterview().getId(),
                CHECKPOINT_IDS[0]);
        CheckpointView view = checkpointProgressService.submitCode(command);
        sharedState.setCurrentCheckpointView(view);
    }

    @When("I submit correct code for all 4 checkpoints")
    public void iSubmitCorrectCodeForAll4Checkpoints() {
        TestCodeExecutorConfig.TestContainerManager.forcePass();
        for (String checkpointId : CHECKPOINT_IDS) {
            var command = new SubmitCodeCommand(
                    sharedState.getCurrentInterview().getId(),
                    checkpointId);
            checkpointProgressService.submitCode(command);
        }
    }

    @Then("the interview should be automatically completed")
    public void theInterviewShouldBeAutomaticallyCompleted() {
        Interview interview = interviewRepository
                .findById(sharedState.getCurrentInterview().getId())
                .orElseThrow();
        assertThat(interview.getStatus()).isEqualTo("COMPLETED");
    }
}
