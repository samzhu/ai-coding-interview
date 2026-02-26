package com.interview.interview.interfaces.rest;

import com.interview.interview.application.InterviewService;
import com.interview.interview.domain.Interview;
import com.interview.interview.domain.InterviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({InterviewController.class, InterviewExceptionHandler.class})
@DisplayName("InterviewController REST API 測試")
class InterviewControllerTest {

    private static final String QUESTION_ID = "question1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InterviewService service;

    @Test
    @DisplayName("POST /api/v1/interviews 成功建立面試，回傳 201")
    void shouldCreateInterviewAndReturn201() throws Exception {
        Interview created = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Java Backend Interview", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        when(service.createInterview(any())).thenReturn(created);

        var request = new CreateInterviewRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                "Java Backend Interview", Instant.now().plusSeconds(3600), QUESTION_ID, null);

        mockMvc.perform(post("/api/v1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Java Backend Interview"))
                .andExpect(jsonPath("$.status").value(InterviewStatus.SCHEDULED.name()));
    }

    @Test
    @DisplayName("POST /api/v1/interviews 缺少 title 時回傳 400")
    void shouldReturn400WhenTitleIsMissing() throws Exception {
        String body = """
                {
                    "candidateId": "%s",
                    "interviewerId": "%s",
                    "scheduledAt": "%s",
                    "questionId": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(3600), QUESTION_ID);

        mockMvc.perform(post("/api/v1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/interviews/{id}/start 成功，回傳 202 (容器異步初始化)")
    void shouldStartInterviewAndReturn202() throws Exception {
        Interview inProgress = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        inProgress.start();
        inProgress.setContainerStatus("INITIALIZING");
        when(service.startInterview(any())).thenReturn(inProgress);

        mockMvc.perform(post("/api/v1/interviews/{id}/start", UUID.randomUUID()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(InterviewStatus.IN_PROGRESS.name()))
                .andExpect(jsonPath("$.containerStatus").value("INITIALIZING"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id}/container-status 回傳容器狀態")
    void shouldReturnContainerStatus() throws Exception {
        UUID interviewId = UUID.randomUUID();
        Interview inProgress = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        inProgress.start();
        inProgress.setContainerStatus("READY");
        when(service.findById(interviewId)).thenReturn(inProgress);

        mockMvc.perform(get("/api/v1/interviews/{id}/container-status", interviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.containerReady").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/interviews/{id}/start 非 SCHEDULED 狀態，回傳 409")
    void shouldReturn409WhenStartingNonScheduledInterview() throws Exception {
        when(service.startInterview(any()))
                .thenThrow(new IllegalStateException("Cannot start interview with status IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/interviews/{id}/start", UUID.randomUUID()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/interviews/{id}/complete 成功，回傳 200")
    void shouldCompleteInterviewAndReturn200() throws Exception {
        Interview completed = Interview.schedule(
                UUID.randomUUID(), UUID.randomUUID(),
                "Test", Instant.now().plusSeconds(3600), QUESTION_ID, null);
        completed.start();
        completed.complete();
        when(service.completeInterview(any())).thenReturn(completed);

        mockMvc.perform(post("/api/v1/interviews/{id}/complete", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(InterviewStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id}/time-remaining 回傳剩餘秒數")
    void shouldReturnTimeRemaining() throws Exception {
        UUID interviewId = UUID.randomUUID();
        when(service.getTimeRemaining(interviewId))
                .thenReturn(new TimeRemainingResponse(3550L, 3600L));

        mockMvc.perform(get("/api/v1/interviews/{id}/time-remaining", interviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingSeconds").value(3550))
                .andExpect(jsonPath("$.totalSeconds").value(3600));
    }
}
