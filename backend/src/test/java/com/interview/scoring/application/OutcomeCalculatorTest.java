package com.interview.scoring.application;

import com.interview.interview.domain.CheckpointResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutcomeCalculatorTest {

    OutcomeCalculator calculator = new OutcomeCalculator();

    @Test
    void emptyList_returnsZeroOfZero() {
        var result = calculator.calculate(List.of());
        assertThat(result.passed()).isZero();
        assertThat(result.total()).isZero();
    }

    @Test
    void allPassed_returnsFullScore() {
        var result = calculator.calculate(List.of(
                checkpointWithStatus("PASSED"),
                checkpointWithStatus("PASSED")
        ));
        assertThat(result.passed()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(2);
    }

    @Test
    void mixed_countsOnlyPassed() {
        var result = calculator.calculate(List.of(
                checkpointWithStatus("PASSED"),
                checkpointWithStatus("FAILED"),
                checkpointWithStatus("PENDING")
        ));
        assertThat(result.passed()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(3);
    }

    private CheckpointResult checkpointWithStatus(String status) {
        // 設計說明：直接 mock 而非 build real CheckpointResult，
        // OutcomeCalculator 只在乎 status 字串。
        var mock = Mockito.mock(CheckpointResult.class);
        Mockito.when(mock.getStatus()).thenReturn(status);
        return mock;
    }
}
