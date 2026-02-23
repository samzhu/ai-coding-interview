package com.interview.question.interfaces.rest;

import com.interview.question.CheckpointDetail;
import com.interview.question.QuestionDetail;
import com.interview.question.QuestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({QuestionController.class, QuestionExceptionHandler.class})
@DisplayName("QuestionController REST API 測試")
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService service;

    @Test
    @DisplayName("GET /api/v1/questions 應回傳題目列表")
    void shouldListQuestions() throws Exception {
        UUID id = UUID.randomUUID();
        var detail = new QuestionDetail(id, "Two Sum", "desc", "EASY", "java", null, null, List.of(), List.of());
        when(service.findAllQuestions()).thenReturn(List.of(detail));

        mockMvc.perform(get("/api/v1/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].title").value("Two Sum"));
    }

    @Test
    @DisplayName("GET /api/v1/questions/{id} 不存在時回傳 404")
    void shouldReturn404WhenQuestionNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(service.getQuestion(unknownId))
                .thenThrow(new IllegalArgumentException("Question not found: " + unknownId));

        mockMvc.perform(get("/api/v1/questions/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/questions/{id} 應回傳題目詳情含 checkpoints")
    void shouldGetQuestionWithCheckpoints() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID cpId = UUID.randomUUID();
        var cp = new CheckpointDetail(cpId, 1, "Implement", "desc", "// code", null, true, List.of());
        var detail = new QuestionDetail(id, "Two Sum", "desc", "EASY", "java", null, null, List.of(), List.of(cp));
        when(service.getQuestion(id)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/questions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.checkpoints[0].sequenceNumber").value(1));
    }
}
