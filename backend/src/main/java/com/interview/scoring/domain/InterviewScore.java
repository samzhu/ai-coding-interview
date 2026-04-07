package com.interview.scoring.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root for interview_scores.
 *
 * 設計說明：
 * - 用 of() / empty() / failed() 三個工廠方法表示三種建立情境
 *   (正常 LLM 評分 / 零互動 / LLM 失敗)，protected 構造子防止外部誤建構
 *   不一致狀態。
 * - pilot_judgement_json 欄位由 InterviewScoreSaver (Task 24) 在 save 前
 *   填入序列化後的 JSON 字串；本 aggregate 不直接持有 ObjectMapper。
 * - judge_model 與 judge_prompt_version 寫死成常數，未來改 prompt 時
 *   bump 版本即可在 metadata 中追溯哪些評分用了哪版 prompt。
 */
@Table("interview_scores")
public class InterviewScore {

    public static final String JUDGE_MODEL = "gemini-3.1-pro-preview";
    public static final String JUDGE_PROMPT_VERSION = "v1.1";
    public static final String SCORING_VERSION = "v1";

    @Id
    private UUID id;

    @Version
    private Long version;

    private UUID interviewId;
    private Instant scoredAt;

    private int checkpointsTotal;
    private int checkpointsPassed;
    private Double outcomeScore;

    private Double pilotScore;
    private String pilotVerdict;
    private String pilotHeadline;
    private String pilotSummary;
    private String pilotJudgementJson;

    private String judgeModel;
    private String judgePromptVersion;
    private Integer timelineTokenCount;
    private String judgeErrorReason;
    private String scoringVersion;

    protected InterviewScore() {} // for Spring Data JDBC

    public static InterviewScore of(
            UUID interviewId, int passed, int total,
            PilotJudgement judgement, int timelineTokens) {
        var s = baseRule(interviewId, passed, total);
        s.pilotScore = judgement.pilotScore();
        s.pilotVerdict = judgement.verdict().name();
        s.pilotHeadline = judgement.headline();
        s.pilotSummary = judgement.summary();
        // pilotJudgementJson 留 null，由 InterviewScoreSaver 在 save 前填入序列化結果
        s.judgeModel = JUDGE_MODEL;
        s.judgePromptVersion = JUDGE_PROMPT_VERSION;
        s.timelineTokenCount = timelineTokens;
        return s;
    }

    public static InterviewScore empty(UUID interviewId, int passed, int total) {
        var s = baseRule(interviewId, passed, total);
        s.pilotScore = 1.0;
        s.pilotVerdict = "PASSENGER";
        s.pilotHeadline = "No interaction recorded for this interview.";
        s.pilotSummary = "No interaction recorded.";
        return s;
    }

    public static InterviewScore failed(UUID interviewId, int passed, int total, String reason) {
        var s = baseRule(interviewId, passed, total);
        s.judgeErrorReason = reason;
        return s;
    }

    private static InterviewScore baseRule(UUID interviewId, int passed, int total) {
        var s = new InterviewScore();
        s.interviewId = interviewId;
        s.checkpointsTotal = total;
        s.checkpointsPassed = passed;
        s.outcomeScore = total == 0 ? 0.0 : (double) passed / total;
        s.scoredAt = Instant.now();
        s.scoringVersion = SCORING_VERSION;
        return s;
    }

    // 設計說明：public so InterviewScoreSaver in infrastructure/ can write JSON before persistence.
    // Aggregate enforces no other write paths.
    public void setPilotJudgementJson(String json) {
        this.pilotJudgementJson = json;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getInterviewId() { return interviewId; }
    public Instant getScoredAt() { return scoredAt; }
    public int getCheckpointsTotal() { return checkpointsTotal; }
    public int getCheckpointsPassed() { return checkpointsPassed; }
    public Double getOutcomeScore() { return outcomeScore; }
    public Double getPilotScore() { return pilotScore; }
    public String getPilotVerdict() { return pilotVerdict; }
    public String getPilotHeadline() { return pilotHeadline; }
    public String getPilotSummary() { return pilotSummary; }
    public String getPilotJudgementJson() { return pilotJudgementJson; }
    public String getJudgeModel() { return judgeModel; }
    public String getJudgePromptVersion() { return judgePromptVersion; }
    public Integer getTimelineTokenCount() { return timelineTokenCount; }
    public String getJudgeErrorReason() { return judgeErrorReason; }
    public String getScoringVersion() { return scoringVersion; }
}
