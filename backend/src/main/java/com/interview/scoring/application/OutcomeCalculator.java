package com.interview.scoring.application;

import com.interview.interview.domain.CheckpointResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 設計說明：純規則計算 outcome 軸（passed checkpoints / total）。
 * 沒有 LLM、沒有時間序列分析，可被任意重跑且結果相同。
 * Pilot Score 雙軸的「客觀」那一軸。
 */
@Component
public class OutcomeCalculator {

    public Outcome calculate(List<CheckpointResult> checkpoints) {
        int total = checkpoints.size();
        int passed = (int) checkpoints.stream()
                .filter(c -> "PASSED".equals(c.getStatus()))
                .count();
        return new Outcome(passed, total);
    }

    public record Outcome(int passed, int total) {}
}
