package com.interview.scoring.infrastructure;

import com.interview.scoring.domain.InterviewScore;
import com.interview.scoring.domain.PilotJudgement;
import com.interview.scoring.domain.PilotVerdict;
import com.interview.scoring.domain.SubScore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewScoreSaverTest {

    @Mock InterviewScoreJdbcRepository repository;

    @Test
    void save_withJudgement_serializesAndStoresJson() {
        var saver = new InterviewScoreSaver(repository, new ObjectMapper());
        var judgement = sampleJudgement();
        var score = InterviewScore.of(UUID.randomUUID(), 3, 3, judgement, 12000);
        when(repository.save(any(InterviewScore.class))).thenAnswer(inv -> inv.getArgument(0));

        saver.save(score, judgement);

        var captor = ArgumentCaptor.forClass(InterviewScore.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPilotJudgementJson())
                .isNotNull()
                .contains("DRIVER")
                .contains("pilotScore");
    }

    @Test
    void save_withoutJudgement_doesNotTouchJsonField() {
        var saver = new InterviewScoreSaver(repository, new ObjectMapper());
        var score = InterviewScore.empty(UUID.randomUUID(), 0, 0);
        when(repository.save(any(InterviewScore.class))).thenAnswer(inv -> inv.getArgument(0));

        saver.save(score);

        verify(repository).save(any(InterviewScore.class));
        assertThat(score.getPilotJudgementJson()).isNull();
    }

    private static PilotJudgement sampleJudgement() {
        var sub = new SubScore(3.5, "good", List.of(), List.of());
        return new PilotJudgement(
                3.5, PilotVerdict.DRIVER, "h", "s",
                new PilotJudgement.SubDimensions(sub, sub, sub, sub));
    }
}
