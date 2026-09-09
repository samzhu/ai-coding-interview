package com.interview.monitoring.interfaces.rest;

import com.interview.WebMvcTestSliceConfig;
import com.interview.monitoring.infrastructure.ActivityEventJdbcRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityEventController.class)
@Import(WebMvcTestSliceConfig.class)
class ActivityEventControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean ActivityEventJdbcRepository repository;

    @Test
    void postActivity_returns201() throws Exception {
        var interviewId = UUID.randomUUID();
        // Build JSON manually to avoid ObjectMapper injection issues in WebMvcTest
        String body = """
                {
                  "eventType": "file.opened",
                  "payload": {"filePath": "solution.py", "checkpointId": "cp-1"}
                }
                """;

        mvc.perform(post("/api/v1/interviews/{id}/activity", interviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
