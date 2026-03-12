package com.interview.monitoring.interfaces.rest;

import com.interview.monitoring.application.InterviewMonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.interview.WebMvcTestSliceConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MonitoringController.class, MonitoringExceptionHandler.class})
@Import(WebMvcTestSliceConfig.class)
@DisplayName("MonitoringController REST API 測試")
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterviewMonitoringService monitoringService;

    @Test
    @DisplayName("GET /monitor/stream 應回傳 text/event-stream content type")
    void shouldReturnSseStream() throws Exception {
        UUID interviewId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(1000L);
        emitter.complete();

        when(monitoringService.subscribe(any())).thenReturn(emitter);

        mockMvc.perform(get("/api/v1/interviews/{id}/monitor/stream", interviewId)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted());
    }

    @Test
    @DisplayName("POST /activity/code 應接受 JSON body 並回傳 200")
    void shouldAcceptCodeActivityAndReturn200() throws Exception {
        UUID interviewId = UUID.randomUUID();
        String body = """
                {
                    "filePath": "main.py",
                    "code": "print('hello')"
                }
                """;

        mockMvc.perform(post("/api/v1/interviews/{id}/activity/code", interviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
