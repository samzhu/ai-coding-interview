package com.interview.interview.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CheckpointResult 聚合根行為")
class CheckpointResultTest {

    private final UUID interviewId = UUID.randomUUID();
    private final String checkpointId = "1";

    @Test
    @DisplayName("建立 PENDING 狀態的 CheckpointResult")
    void shouldCreatePendingResult() {
        CheckpointResult result = CheckpointResult.createPending(interviewId, checkpointId, 1);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getCheckpointResultStatus()).isEqualTo(CheckpointResultStatus.PENDING);
        assertThat(result.getInterviewId()).isEqualTo(interviewId);
        assertThat(result.getCheckpointId()).isEqualTo(checkpointId);
        assertThat(result.getCheckpointSequence()).isEqualTo(1);
    }

    @Test
    @DisplayName("PENDING → IN_PROGRESS 轉換應成功")
    void shouldTransitionFromPendingToInProgress() {
        CheckpointResult result = CheckpointResult.createPending(interviewId, checkpointId, 1);

        result.startProgress("// my code");

        assertThat(result.getCheckpointResultStatus()).isEqualTo(CheckpointResultStatus.IN_PROGRESS);
        assertThat(result.getSubmittedCode()).isEqualTo("// my code");
    }

    @Test
    @DisplayName("IN_PROGRESS → PASSED 轉換應成功")
    void shouldTransitionFromInProgressToPassed() {
        CheckpointResult result = CheckpointResult.createPending(interviewId, checkpointId, 1);
        result.startProgress("// code");

        result.markPassed("PASS");

        assertThat(result.getCheckpointResultStatus()).isEqualTo(CheckpointResultStatus.PASSED);
        assertThat(result.getPassedAt()).isNotNull();
    }

    @Test
    @DisplayName("IN_PROGRESS → FAILED 轉換應成功")
    void shouldTransitionFromInProgressToFailed() {
        CheckpointResult result = CheckpointResult.createPending(interviewId, checkpointId, 1);
        result.startProgress("// wrong code");

        result.markFailed("FAIL");

        assertThat(result.getCheckpointResultStatus()).isEqualTo(CheckpointResultStatus.FAILED);
    }

    @Test
    @DisplayName("FAILED → IN_PROGRESS 允許重試")
    void shouldAllowRetryFromFailed() {
        CheckpointResult result = CheckpointResult.createPending(interviewId, checkpointId, 1);
        result.startProgress("// wrong");
        result.markFailed("FAIL");

        result.startProgress("// fixed");

        assertThat(result.getCheckpointResultStatus()).isEqualTo(CheckpointResultStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("PASSED 狀態不允許再次轉換")
    void shouldNotAllowTransitionFromPassed() {
        CheckpointResult result = CheckpointResult.createPending(interviewId, checkpointId, 1);
        result.startProgress("// code");
        result.markPassed("PASS");

        assertThatThrownBy(() -> result.startProgress("// retry"))
                .isInstanceOf(IllegalStateException.class);
    }
}
