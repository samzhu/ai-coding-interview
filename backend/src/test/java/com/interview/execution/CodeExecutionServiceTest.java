package com.interview.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CodeExecutionService 業務邏輯")
class CodeExecutionServiceTest {

    @Mock
    private CodeExecutor executor;

    @InjectMocks
    private CodeExecutionService service;

    @Test
    @DisplayName("執行程式碼應委派給 CodeExecutor")
    void shouldDelegateToExecutor() {
        var request = new CodeExecutionRequest("python", "print('hello')", "", 10);
        var expected = new CodeExecutionResult(0, "hello", "", 100, ExecutionStatus.SUCCESS);
        when(executor.execute(any())).thenReturn(expected);

        CodeExecutionResult result = service.execute(request);

        assertThat(result.status()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("hello");
    }
}
