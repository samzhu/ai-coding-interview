package com.interview.interview.bdd;

import com.interview.interview.application.CheckpointProgressService;
import com.interview.interview.application.CheckpointProgressService.CheckpointView;
import com.interview.interview.application.CreateInterviewCommand;
import com.interview.interview.application.InterviewService;
import com.interview.interview.domain.Interview;
import com.interview.interview.domain.InterviewStatus;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import com.interview.invitation.application.InvitationService;
import com.interview.invitation.domain.Invitation;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EndToEndStepDefinitions {

    private static final String SEED_QUESTION_ID = "question1";

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private CheckpointProgressService checkpointProgressService;

    @Autowired
    private InterviewRepository interviewRepository;

    // Shared state — injected from InterviewStepDefinitions via Spring context
    @Autowired
    private SharedInterviewState sharedState;

    @When("the interviewer creates an invitation for the interview")
    public void theInterviewerCreatesAnInvitationForTheInterview() {
        Invitation invitation = invitationService.createOrGetInvitation(sharedState.getCurrentInterview().getId());
        sharedState.setCurrentInvitation(invitation);
    }

    @Then("an invitation token should be generated")
    public void anInvitationTokenShouldBeGenerated() {
        assertThat(sharedState.getCurrentInvitation()).isNotNull();
        assertThat(sharedState.getCurrentInvitation().getToken()).isNotBlank();
    }

    @When("the candidate joins using the invitation token")
    public void theCandidateJoinsUsingTheInvitationToken() {
        invitationService.joinByToken(sharedState.getCurrentInvitation().getToken());
        // 重新從 DB 讀取 interview，因為 CandidateJoinedEvent 觸發了 startInterview
        Interview updated = interviewRepository.findById(sharedState.getCurrentInterview().getId()).orElseThrow();
        sharedState.setCurrentInterview(updated);
    }

    @Then("the interview should automatically start with status {string}")
    public void theInterviewShouldAutomaticallyStartWithStatus(String expectedStatus) {
        Interview interview = interviewRepository.findById(sharedState.getCurrentInterview().getId()).orElseThrow();
        assertThat(interview.getStatus()).isEqualTo(expectedStatus);
        sharedState.setCurrentInterview(interview);
    }

    @And("the first checkpoint should be available")
    public void theFirstCheckpointShouldBeAvailable() {
        CheckpointView view = checkpointProgressService.getCurrentCheckpoint(sharedState.getCurrentInterview().getId());
        assertThat(view).isNotNull();
        assertThat(view.sequenceNumber()).isEqualTo(1);
    }

    @When("the candidate requests the current checkpoint")
    public void theCandidateRequestsTheCurrentCheckpoint() {
        CheckpointView view = checkpointProgressService.getCurrentCheckpoint(sharedState.getCurrentInterview().getId());
        sharedState.setCurrentCheckpointView(view);
    }

    @And("the checkpoint should have a description")
    public void theCheckpointShouldHaveADescription() {
        assertThat(sharedState.getCurrentCheckpointView().description()).isNotBlank();
    }
}
