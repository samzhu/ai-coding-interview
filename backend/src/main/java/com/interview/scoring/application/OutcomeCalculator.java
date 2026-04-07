package com.interview.scoring.application;

import com.interview.interview.CheckpointResultView;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 設計說明：純規則計算 outcome 軸（passed checkpoints / total）。
 * 沒有 LLM、沒有時間序列分析，可被任意重跑且結果相同。
 * Pilot Score 雙軸的「客觀」那一軸。
 *
 * 接受 CheckpointResultView (DTO) 而非 domain entity — 跨模組邊界尊重
 * Spring Modulith 規則：scoring 模組不直接認識 interview 模組的內部 entity。
 */
@Component
public class OutcomeCalculator {

    public Outcome calculate(List<CheckpointResultView> checkpoints) {
        int total = checkpoints.size();
        int passed = (int) checkpoints.stream()
                .filter(c -> "PASSED".equals(c.status()))
                .count();
        return new Outcome(passed, total);
    }

    public record Outcome(int passed, int total) {}
}
