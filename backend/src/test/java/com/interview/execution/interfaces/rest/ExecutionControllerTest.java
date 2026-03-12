package com.interview.execution.interfaces.rest;

import com.interview.execution.CodeExecutionResult;
import com.interview.execution.CodeExecutionService;
import com.interview.execution.ExecutionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.interview.WebMvcTestSliceConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ExecutionController.class, ExecutionExceptionHandler.class})
@Import(WebMvcTestSliceConfig.class)
@DisplayName("ExecutionController REST API 測試")
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CodeExecutionService service;

    @Test
    @DisplayName("POST /api/v1/executions 執行 Python 程式碼成功")
    void shouldExecutePythonCode() throws Exception {
        var result = new CodeExecutionResult(0, "hello", "", 50, ExecutionStatus.SUCCESS);
        when(service.execute(any())).thenReturn(result);

        String body = """
                {
                    "language": "python",
                    "code": "print('hello')",
                    "stdin": "",
                    "timeoutSeconds": 10
                }
                """;

        mockMvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stdout").value("hello"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/v1/executions 缺少 language 時回傳 400")
    void shouldReturn400WhenLanguageMissing() throws Exception {
        String body = """
                {
                    "code": "print('hello')",
                    "timeoutSeconds": 10
                }
                """;

        mockMvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
