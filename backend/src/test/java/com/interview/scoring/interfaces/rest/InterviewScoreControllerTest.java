package com.interview.scoring.interfaces.rest;

import com.interview.WebMvcTestSliceConfig;
import com.interview.scoring.domain.InterviewScore;
import com.interview.scoring.domain.PilotJudgement;
import com.interview.scoring.domain.PilotVerdict;
import com.interview.scoring.domain.SubScore;
import com.interview.scoring.infrastructure.InterviewScoreJdbcRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterviewScoreController.class)
@Import(WebMvcTestSliceConfig.class)
class InterviewScoreControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean InterviewScoreJdbcRepository repository;

    @Test
    void get_returns200WithScore_whenExists() throws Exception {
        var interviewId = UUID.randomUUID();
        var sub = new SubScore(3.5, "good", List.of(), List.of());
        var judgement = new PilotJudgement(
                3.5, PilotVerdict.DRIVER, "head", "summ",
                new PilotJudgement.SubDimensions(sub, sub, sub, sub));
        var score = InterviewScore.of(interviewId, 3, 3, judgement, 1000);
        when(repository.findByInterviewId(interviewId)).thenReturn(Optional.of(score));

        mvc.perform(get("/api/v1/interviews/{id}/score", interviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pilotVerdict").value("DRIVER"))
                .andExpect(jsonPath("$.outcomeScore").value(1.0))
                .andExpect(jsonPath("$.pilotHeadline").value("head"));
    }

    @Test
    void get_returns404_whenNotExists() throws Exception {
        var interviewId = UUID.randomUUID();
        when(repository.findByInterviewId(interviewId)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/interviews/{id}/score", interviewId))
                .andExpect(status().isNotFound());
    }
}
