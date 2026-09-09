package com.interview.scoring.interfaces.rest;

import com.interview.scoring.domain.InterviewScore;
import com.interview.scoring.infrastructure.InterviewScoreJdbcRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 設計說明：scoring 模組對外的 REST endpoint。前端列表頁與詳情頁讀取 pilot_headline /
 * pilot_summary，深入查 evidence 時 parse pilot_judgement_json (raw JSON 直接傳給前端)。
 * GET only — 評分由 ScoringOrchestrator 自動觸發，不開放手動 POST。
 */
@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/score")
public class InterviewScoreController {

    private final InterviewScoreJdbcRepository repository;

    InterviewScoreController(InterviewScoreJdbcRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<InterviewScoreResponse> get(@PathVariable UUID interviewId) {
        return repository.findByInterviewId(interviewId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private InterviewScoreResponse toResponse(InterviewScore s) {
        return new InterviewScoreResponse(
                s.getInterviewId(), s.getScoredAt(),
                s.getCheckpointsTotal(), s.getCheckpointsPassed(), s.getOutcomeScore(),
                s.getPilotScore(), s.getPilotVerdict(), s.getPilotHeadline(),
                s.getPilotSummary(), s.getPilotJudgementJson(),
                s.getJudgeModel(), s.getJudgeErrorReason());
    }
}
