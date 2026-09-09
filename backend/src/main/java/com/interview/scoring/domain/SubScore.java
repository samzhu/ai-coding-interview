package com.interview.scoring.domain;

import java.util.List;

/**
 * 設計說明：對應 Pilot Score 4 個 sub-dimensions 之一的評分結果。
 * - score: 1.0 ~ 4.0 (1=No Hire, 4=Strong Hire)
 * - rationale: 1-2 句口語白話解釋為何給這個分數
 * - positive/negativeEvidence: 至少各 1 條，最多各 3 條，引用 timeline 時間戳
 */
public record SubScore(
        double score,
        String rationale,
        List<Evidence> positiveEvidence,
        List<Evidence> negativeEvidence
) {}
