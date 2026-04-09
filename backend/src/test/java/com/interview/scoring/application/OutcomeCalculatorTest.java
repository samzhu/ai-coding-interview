package com.interview.scoring.application;

import com.interview.interview.CheckpointResultView;
import org.junit.jupiter.api.Test;

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
                view("PASSED"),
                view("PASSED")
        ));
        assertThat(result.passed()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(2);
    }

    @Test
    void mixed_countsOnlyPassed() {
        var result = calculator.calculate(List.of(
                view("PASSED"),
                view("FAILED"),
                view("PENDING")
        ));
        assertThat(result.passed()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(3);
    }

    private CheckpointResultView view(String status) {
        // 設計說明：record 是 immutable，直接 build 比 mock 簡單。
        // OutcomeCalculator 只在乎 status，其他欄位填 null/0 即可。
        return new CheckpointResultView("cp-id", 0, status, null, null, null, 0, null);
    }
}
