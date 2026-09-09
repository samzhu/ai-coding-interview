package com.interview.scoring.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewScoreTest {

    @Test
    void empty_writesPassengerWithZeroOutcome() {
        var id = UUID.randomUUID();
        var score = InterviewScore.empty(id, 0, 0);

        assertThat(score.getInterviewId()).isEqualTo(id);
        assertThat(score.getOutcomeScore()).isEqualTo(0.0);
        assertThat(score.getPilotScore()).isEqualTo(1.0);
        assertThat(score.getPilotVerdict()).isEqualTo("PASSENGER");
        assertThat(score.getPilotHeadline()).contains("No interaction");
    }

    @Test
    void of_withJudgement_calculatesOutcomeAndStoresPilot() {
        var id = UUID.randomUUID();
        var judgement = sampleJudgement();

        var score = InterviewScore.of(id, 3, 3, judgement, 12000);

        assertThat(score.getCheckpointsTotal()).isEqualTo(3);
        assertThat(score.getCheckpointsPassed()).isEqualTo(3);
        assertThat(score.getOutcomeScore()).isEqualTo(1.0);
        assertThat(score.getPilotScore()).isEqualTo(3.5);
        assertThat(score.getPilotVerdict()).isEqualTo("DRIVER");
        assertThat(score.getJudgeModel()).isEqualTo("gemini-3.1-pro-preview");
        assertThat(score.getJudgePromptVersion()).isEqualTo("v1.1");
        assertThat(score.getTimelineTokenCount()).isEqualTo(12000);
        assertThat(score.getPilotHeadline()).isEqualTo("h");
        assertThat(score.getPilotSummary()).isEqualTo("s");
    }

    @Test
    void failed_writesOutcomeOnlyWithErrorReason() {
        var id = UUID.randomUUID();
        var score = InterviewScore.failed(id, 2, 3, "Gemini timeout after 3 retries");
        assertThat(score.getOutcomeScore()).isCloseTo(0.667, org.assertj.core.data.Offset.offset(0.01));
        assertThat(score.getPilotScore()).isNull();
        assertThat(score.getJudgeErrorReason()).contains("timeout");
    }

    @Test
    void of_zeroTotal_outcomeScoreIsZero() {
        var id = UUID.randomUUID();
        var score = InterviewScore.of(id, 0, 0, sampleJudgement(), 100);
        assertThat(score.getOutcomeScore()).isEqualTo(0.0);
    }

    private static PilotJudgement sampleJudgement() {
        var sub = new SubScore(3.5, "good", List.of(), List.of());
        return new PilotJudgement(
                3.5, PilotVerdict.DRIVER, "h", "s",
                new PilotJudgement.SubDimensions(sub, sub, sub, sub)
        );
    }
}
