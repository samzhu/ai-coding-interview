package com.interview.interview.bdd;

import com.interview.interview.application.CreateInterviewCommand;
import com.interview.interview.application.InterviewService;
import com.interview.interview.domain.Interview;
import com.interview.interview.domain.InterviewStatus;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class InterviewStepDefinitions {

    private static final UUID SEED_QUESTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private InterviewService service;

    @Autowired
    private CapturedEvents capturedEvents;

    @Autowired
    private SharedInterviewState sharedState;

    private UUID currentCandidateId;
    private UUID currentInterviewerId;
    private Throwable caughtException;

    @Before
    public void setUp() {
        capturedEvents.clear();
        sharedState.reset();
        caughtException = null;
    }

    @Given("the system is ready to accept interview requests")
    public void theSystemIsReadyToAcceptInterviewRequests() {
        // Spring context is loaded — nothing additional needed
    }

    @Given("a candidate with id {string}")
    public void aCandidateWithId(String candidateId) {
        this.currentCandidateId = UUID.nameUUIDFromBytes(candidateId.getBytes());
    }

    @Given("an interviewer with id {string}")
    public void anInterviewerWithId(String interviewerId) {
        this.currentInterviewerId = UUID.nameUUIDFromBytes(interviewerId.getBytes());
    }

    @When("I create an interview with title {string} scheduled at {string}")
    public void iCreateAnInterviewWithTitle(String title, String scheduledAt) {
        var command = new CreateInterviewCommand(
                currentCandidateId, currentInterviewerId, title, Instant.parse(scheduledAt), SEED_QUESTION_ID, null, null);
        Interview interview = service.createInterview(command);
        sharedState.setCurrentInterview(interview);
    }

    @Then("the interview should be created with status {string}")
    public void theInterviewShouldBeCreatedWithStatus(String expectedStatus) {
        assertThat(sharedState.getCurrentInterview()).isNotNull();
        assertThat(sharedState.getCurrentInterview().getStatus()).isEqualTo(expectedStatus);
    }

    @And("the interview should have title {string}")
    public void theInterviewShouldHaveTitle(String expectedTitle) {
        assertThat(sharedState.getCurrentInterview().getTitle()).isEqualTo(expectedTitle);
    }

    @When("I attempt to create an interview without a title")
    public void iAttemptToCreateAnInterviewWithoutATitle() {
        caughtException = catchThrowable(() -> {
            var command = new CreateInterviewCommand(
                    currentCandidateId, UUID.randomUUID(), null, Instant.now().plusSeconds(3600), SEED_QUESTION_ID, null, null);
            service.createInterview(command);
        });
    }

    @Then("the creation should fail with a validation error")
    public void theCreationShouldFailWithAValidationError() {
        assertThat(caughtException).isNotNull();
        assertThat(caughtException).isInstanceOfAny(IllegalArgumentException.class);
    }

    @And("the error message should mention {string}")
    public void theErrorMessageShouldMention(String keyword) {
        assertThat(caughtException.getMessage()).containsIgnoringCase(keyword);
    }

    @Given("an existing interview with status {string}")
    public void anExistingInterviewWithStatus(String status) {
        var command = new CreateInterviewCommand(
                UUID.randomUUID(), UUID.randomUUID(),
                "BDD Test Interview", Instant.now().plusSeconds(3600), SEED_QUESTION_ID, null, null);
        Interview interview = service.createInterview(command);

        if (InterviewStatus.IN_PROGRESS.name().equals(status)) {
            interview = service.startInterview(interview.getId());
        }
        sharedState.setCurrentInterview(interview);
    }

    @When("the interviewer starts the interview")
    public void theInterviewerStartsTheInterview() {
        Interview interview = service.startInterview(sharedState.getCurrentInterview().getId());
        sharedState.setCurrentInterview(interview);
    }

    @Then("the interview status should change to {string}")
    public void theInterviewStatusShouldChangeTo(String expectedStatus) {
        assertThat(sharedState.getCurrentInterview().getStatus()).isEqualTo(expectedStatus);
    }

    @And("the interview started_at should be recorded")
    public void theInterviewStartedAtShouldBeRecorded() {
        assertThat(sharedState.getCurrentInterview().getStartedAt()).isNotNull();
    }

    @When("the interviewer attempts to start the interview again")
    public void theInterviewerAttemptsToStartTheInterviewAgain() {
        caughtException = catchThrowable(() ->
                service.startInterview(sharedState.getCurrentInterview().getId()));
    }

    @Then("the operation should fail with an illegal state error")
    public void theOperationShouldFailWithAnIllegalStateError() {
        assertThat(caughtException)
                .isNotNull()
                .isInstanceOf(IllegalStateException.class);
    }

    @And("the interview status should remain {string}")
    public void theInterviewStatusShouldRemain(String expectedStatus) {
        assertThat(sharedState.getCurrentInterview().getStatus()).isEqualTo(expectedStatus);
    }

    @When("the interviewer completes the interview")
    public void theInterviewerCompletesTheInterview() {
        Interview interview = service.completeInterview(sharedState.getCurrentInterview().getId());
        sharedState.setCurrentInterview(interview);
    }

    @And("the interview completed_at should be recorded")
    public void theInterviewCompletedAtShouldBeRecorded() {
        assertThat(sharedState.getCurrentInterview().getCompletedAt()).isNotNull();
    }

    @And("an evaluation event should be published")
    public void anEvaluationEventShouldBePublished() {
        assertThat(capturedEvents.hasCompletedEventFor(sharedState.getCurrentInterview().getId())).isTrue();
    }
}
