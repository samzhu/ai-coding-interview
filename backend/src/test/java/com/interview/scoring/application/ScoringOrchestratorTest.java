package com.interview.scoring.application;

import com.interview.ai.AiConversationQueryService;
import com.interview.ai.AiConversationView;
import com.interview.interview.CheckpointResultQueryService;
import com.interview.interview.CheckpointResultView;
import com.interview.interview.InterviewCompletedEvent;
import com.interview.monitoring.ActivityEventQueryService;
import com.interview.scoring.domain.InterviewScore;
import com.interview.scoring.domain.PilotJudgement;
import com.interview.scoring.domain.PilotVerdict;
import com.interview.scoring.domain.SubScore;
import com.interview.scoring.infrastructure.InterviewScoreSaver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringOrchestratorTest {

    @Mock CheckpointResultQueryService checkpointService;
    @Mock AiConversationQueryService conversationService;
    @Mock ActivityEventQueryService activityService;
    @Mock OutcomeCalculator outcomeCalculator;
    @Mock TimelineBuilder timelineBuilder;
    @Mock GeminiPilotJudge geminiJudge;
    @Mock InterviewScoreSaver saver;

    @InjectMocks ScoringOrchestrator orchestrator;

    @Test
    void onCompleted_zeroInteraction_savesEmptyAndSkipsLlm() {
        var interviewId = UUID.randomUUID();
        when(checkpointService.findByInterviewId(interviewId)).thenReturn(List.of());
        when(conversationService.findByInterviewId(interviewId)).thenReturn(List.of());
        when(outcomeCalculator.calculate(any())).thenReturn(new OutcomeCalculator.Outcome(0, 0));

        orchestrator.onInterviewCompleted(new InterviewCompletedEvent(interviewId));

        verify(geminiJudge, never()).judge(any());
        var captor = ArgumentCaptor.forClass(InterviewScore.class);
        verify(saver).save(captor.capture());
        assertThat(captor.getValue().getPilotVerdict()).isEqualTo("PASSENGER");
    }

    @Test
    void onCompleted_normalCase_orchestratesFullPipeline() {
        var interviewId = UUID.randomUUID();
        var msg = new AiConversationView("USER", "hi", Instant.parse("2026-04-07T10:00:00Z"));
        var checkpoint = new CheckpointResultView("cp-1", 1, "PASSED", null, null, null, 1, "ok");

        when(checkpointService.findByInterviewId(interviewId)).thenReturn(List.of(checkpoint));
        when(conversationService.findByInterviewId(interviewId)).thenReturn(List.of(msg));
        when(activityService.findByInterviewId(interviewId)).thenReturn(List.of());
        when(outcomeCalculator.calculate(any())).thenReturn(new OutcomeCalculator.Outcome(1, 1));
        when(timelineBuilder.build(any(), any(), any(), any(), any(), any()))
                .thenReturn(new TimelineBuilder.Timeline("text", 5000));
        when(geminiJudge.judge("text")).thenReturn(sampleJudgement());

        orchestrator.onInterviewCompleted(new InterviewCompletedEvent(interviewId));

        verify(geminiJudge).judge("text");
        var captor = ArgumentCaptor.forClass(InterviewScore.class);
        verify(saver).save(captor.capture(), any(PilotJudgement.class));
        assertThat(captor.getValue().getPilotVerdict()).isEqualTo("DRIVER");
    }

    @Test
    void onCompleted_judgeThrows_savesFailedScoreWithReason() {
        var interviewId = UUID.randomUUID();
        var msg = new AiConversationView("USER", "hi", Instant.parse("2026-04-07T10:00:00Z"));
        var checkpoint = new CheckpointResultView("cp-1", 1, "FAILED", null, null, null, 1, "err");

        when(checkpointService.findByInterviewId(interviewId)).thenReturn(List.of(checkpoint));
        when(conversationService.findByInterviewId(interviewId)).thenReturn(List.of(msg));
        when(activityService.findByInterviewId(interviewId)).thenReturn(List.of());
        when(outcomeCalculator.calculate(any())).thenReturn(new OutcomeCalculator.Outcome(0, 1));
        when(timelineBuilder.build(any(), any(), any(), any(), any(), any()))
                .thenReturn(new TimelineBuilder.Timeline("text", 1000));
        when(geminiJudge.judge(any())).thenThrow(new RuntimeException("Gemini timeout"));

        orchestrator.onInterviewCompleted(new InterviewCompletedEvent(interviewId));

        var captor = ArgumentCaptor.forClass(InterviewScore.class);
        verify(saver).save(captor.capture());
        assertThat(captor.getValue().getJudgeErrorReason()).contains("Gemini timeout");
        assertThat(captor.getValue().getPilotScore()).isNull();
    }

    private static PilotJudgement sampleJudgement() {
        var sub = new SubScore(3.5, "good", List.of(), List.of());
        return new PilotJudgement(
                3.5, PilotVerdict.DRIVER, "h", "s",
                new PilotJudgement.SubDimensions(sub, sub, sub, sub));
    }
}
