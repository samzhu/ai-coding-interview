package com.interview.scoring.application;

import com.interview.ai.AiConversationQueryService;
import com.interview.ai.AiConversationView;
import com.interview.interview.CheckpointResultQueryService;
import com.interview.interview.CheckpointResultView;
import com.interview.interview.InterviewCompletedEvent;
import com.interview.monitoring.ActivityEventQueryService;
import com.interview.monitoring.ActivityEventView;
import com.interview.scoring.domain.InterviewScore;
import com.interview.scoring.infrastructure.InterviewScoreSaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 監聽 InterviewCompletedEvent，協調雙軸評分流程：
 *  1. Outcome 軸 — 純規則 (OutcomeCalculator)
 *  2. Pilot 軸 — Gemini Judge (TimelineBuilder + GeminiPilotJudge)
 *
 * 設計說明：用 @ApplicationModuleListener (async + REQUIRES_NEW + Event Publication
 * Registry retry) 而非同步 @EventListener，因為：
 *  - 不阻塞 InterviewService 的 completeInterview transaction
 *  - Gemini 呼叫可能 60s+，不能拖死面試結束流程
 *  - 失敗後 Spring Modulith 自動重試
 *
 * 早退邏輯：完全沒互動的面試直接寫 PASSENGER，省 LLM 成本。
 * 失敗處理：Gemini 呼叫拋例外時寫 InterviewScore.failed() 記錄錯誤原因，不 propagate。
 */
@Service
public class ScoringOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ScoringOrchestrator.class);

    private final CheckpointResultQueryService checkpointService;
    private final AiConversationQueryService conversationService;
    private final ActivityEventQueryService activityService;
    private final OutcomeCalculator outcomeCalculator;
    private final TimelineBuilder timelineBuilder;
    private final GeminiPilotJudge geminiJudge;
    private final InterviewScoreSaver saver;

    @Autowired
    ScoringOrchestrator(
            CheckpointResultQueryService checkpointService,
            AiConversationQueryService conversationService,
            ActivityEventQueryService activityService,
            OutcomeCalculator outcomeCalculator,
            TimelineBuilder timelineBuilder,
            GeminiPilotJudge geminiJudge,
            InterviewScoreSaver saver) {
        this.checkpointService = checkpointService;
        this.conversationService = conversationService;
        this.activityService = activityService;
        this.outcomeCalculator = outcomeCalculator;
        this.timelineBuilder = timelineBuilder;
        this.geminiJudge = geminiJudge;
        this.saver = saver;
    }

    @ApplicationModuleListener
    public void onInterviewCompleted(InterviewCompletedEvent event) {
        var interviewId = event.interviewId();
        log.info("Scoring interview {}", interviewId);

        List<CheckpointResultView> checkpoints = checkpointService.findByInterviewId(interviewId);
        var outcome = outcomeCalculator.calculate(checkpoints);
        List<AiConversationView> conversations = conversationService.findByInterviewId(interviewId);

        // 早退：完全沒互動的面試
        if (conversations.isEmpty() && checkpoints.isEmpty()) {
            log.info("Interview {} has no interaction; saving empty score", interviewId);
            saver.save(InterviewScore.empty(interviewId, outcome.passed(), outcome.total()));
            return;
        }

        List<ActivityEventView> activityEvents = activityService.findByInterviewId(interviewId);

        // 設計說明：startedAt/endedAt 取對話與事件的時間 envelope。
        // 如果之後 InterviewCompletedEvent 帶上實際面試時間戳，可改成從 event 直接讀。
        var startedAt = earliestTime(conversations, activityEvents);
        var endedAt = Instant.now();

        var timeline = timelineBuilder.build(
                interviewId, startedAt, endedAt, conversations, activityEvents, checkpoints);

        try {
            var judgement = geminiJudge.judge(timeline.text());
            var score = InterviewScore.of(
                    interviewId, outcome.passed(), outcome.total(),
                    judgement, timeline.approxTokenCount());
            saver.save(score, judgement);
            log.info("Scored interview {}: outcome={}/{}, pilot={}",
                    interviewId, outcome.passed(), outcome.total(), judgement.verdict());
        } catch (Exception e) {
            log.error("Gemini judge failed for interview {}", interviewId, e);
            saver.save(InterviewScore.failed(
                    interviewId, outcome.passed(), outcome.total(),
                    e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    private static Instant earliestTime(
            List<AiConversationView> conversations, List<ActivityEventView> events) {
        Instant earliest = Instant.now();
        for (var c : conversations) {
            if (c.createdAt() != null && c.createdAt().isBefore(earliest)) earliest = c.createdAt();
        }
        for (var e : events) {
            if (e.occurredAt() != null && e.occurredAt().isBefore(earliest)) earliest = e.occurredAt();
        }
        return earliest;
    }
}
