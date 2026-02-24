package com.interview.interview.bdd;

import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonStepDefinitions {

    @Autowired
    private SharedInterviewState sharedState;

    @Then("the checkpoint status should be {string}")
    public void theCheckpointStatusShouldBe(String expectedStatus) {
        assertThat(sharedState.getCurrentCheckpointView().status()).isEqualTo(expectedStatus);
    }
}
