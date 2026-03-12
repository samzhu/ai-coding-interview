package com.interview.question.interfaces.rest;

import com.interview.question.QuestionDetail;
import com.interview.question.QuestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.interview.WebMvcTestSliceConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({QuestionController.class, QuestionExceptionHandler.class})
@Import(WebMvcTestSliceConfig.class)
@DisplayName("QuestionController REST API 測試")
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService service;

    @Test
    @DisplayName("GET /api/v1/questions 應回傳題目列表")
    void shouldListQuestions() throws Exception {
        String id = "question1";
        var detail = new QuestionDetail(id, "Two Sum", "desc", "EASY", "java", null, null, null);
        when(service.findAllQuestions()).thenReturn(List.of(detail));

        mockMvc.perform(get("/api/v1/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].title").value("Two Sum"));
    }

    @Test
    @DisplayName("GET /api/v1/questions/{id} 不存在時回傳 404")
    void shouldReturn404WhenQuestionNotFound() throws Exception {
        String unknownId = "nonexistent";
        when(service.getQuestion(unknownId))
                .thenThrow(new IllegalArgumentException("Question not found: " + unknownId));

        mockMvc.perform(get("/api/v1/questions/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/questions/{id} 應回傳題目詳情")
    void shouldGetQuestion() throws Exception {
        String id = "question1";
        var detail = new QuestionDetail(id, "Two Sum", "desc", "EASY", "java", null, null, "some-image:latest");
        when(service.getQuestion(id)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/questions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.difficulty").value("EASY"));
    }
}
