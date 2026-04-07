package com.interview.scoring.interfaces.rest;

import java.time.Instant;
import java.util.UUID;

public record InterviewScoreResponse(
        UUID interviewId,
        Instant scoredAt,

        int checkpointsTotal,
        int checkpointsPassed,
        double outcomeScore,

        Double pilotScore,
        String pilotVerdict,
        String pilotHeadline,
        String pilotSummary,
        String pilotJudgementJson,    // raw JSON for frontend to render evidence

        String judgeModel,
        String judgeErrorReason
) {}
