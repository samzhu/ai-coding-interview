package com.interview.scoring.application;

import com.interview.ai.AiConversationView;
import com.interview.interview.CheckpointResultView;
import com.interview.monitoring.ActivityEventView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TimelineBuilderTest {

    private TimelineBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new TimelineBuilder();
    }

    @Test
    void emptyInputs_producesHeaderAndEmptyStats() {
        var timeline = builder.build(
                UUID.randomUUID(),
                Instant.parse("2026-04-07T10:00:00Z"),
                Instant.parse("2026-04-07T10:30:00Z"),
                List.of(), List.of(), List.of()
        );

        assertThat(timeline.text()).contains("=== INTERVIEW TIMELINE ===");
        assertThat(timeline.text()).contains("duration: 30:00");
        assertThat(timeline.text()).contains("checkpoints_total: 0");
        assertThat(timeline.text()).contains("=== AGGREGATE STATS ===");
        assertThat(timeline.text()).contains("total_prompts:           0");
        assertThat(timeline.approxTokenCount()).isGreaterThan(0);
    }

    @Test
    void singleCandidateMessage_appearsAsCandidateLine() {
        var startedAt = Instant.parse("2026-04-07T10:00:00Z");
        var msg = new AiConversationView("USER", "Looking at line 23",
                startedAt.plusSeconds(75));

        var timeline = builder.build(
                UUID.randomUUID(), startedAt, startedAt.plusSeconds(120),
                List.of(msg), List.of(), List.of());

        assertThat(timeline.text()).contains("[01:15] CANDIDATE> \"Looking at line 23\"");
        assertThat(timeline.text()).contains("total_prompts:           1");
    }

    @Test
    void editProposalEvent_appearsAsAiProposeLine() {
        var startedAt = Instant.parse("2026-04-07T10:00:00Z");
        var event = new ActivityEventView(
                startedAt.plusSeconds(130), "ai.edit.proposal",
                Map.of("proposalId", "p-1", "filePath", "solution.py", "diff", "@@ x @@"));

        var timeline = builder.build(
                UUID.randomUUID(), startedAt, startedAt.plusSeconds(180),
                List.of(), List.of(event), List.of());

        assertThat(timeline.text()).contains("[02:10] AI_PROPOSE: solution.py");
        assertThat(timeline.text()).contains("total_proposals:         1");
    }

    @Test
    void acceptAfterPropose_includesReviewSecondsAnnotation() {
        var startedAt = Instant.parse("2026-04-07T10:00:00Z");
        var propose = new ActivityEventView(
                startedAt.plusSeconds(130), "ai.edit.proposal",
                Map.of("proposalId", "p-1", "filePath", "solution.py", "diff", "..."));
        var accept = new ActivityEventView(
                startedAt.plusSeconds(158), "ai.edit.accepted",
                Map.of("proposalId", "p-1"));

        var timeline = builder.build(
                UUID.randomUUID(), startedAt, startedAt.plusSeconds(200),
                List.of(), List.of(propose, accept), List.of());

        assertThat(timeline.text()).containsPattern("AI_ACCEPT.*review_seconds=28");
        assertThat(timeline.text()).doesNotContain("<FAST>");
    }

    @Test
    void fastAccept_getsFastMarkerAndCount() {
        var startedAt = Instant.parse("2026-04-07T10:00:00Z");
        var propose = new ActivityEventView(
                startedAt.plusSeconds(100), "ai.edit.proposal",
                Map.of("proposalId", "p-1", "filePath", "x.py", "diff", "..."));
        var accept = new ActivityEventView(
                startedAt.plusSeconds(101), "ai.edit.accepted",
                Map.of("proposalId", "p-1"));

        var timeline = builder.build(
                UUID.randomUUID(), startedAt, startedAt.plusSeconds(200),
                List.of(), List.of(propose, accept), List.of());

        assertThat(timeline.text()).contains("<FAST>");
        assertThat(timeline.text()).contains("fast_accepts (<5s):      1");
    }

    @Test
    void checkpointPassed_appearsAsCheckpointSubmit() {
        var startedAt = Instant.parse("2026-04-07T10:00:00Z");
        var cp = new CheckpointResultView(
                "cp-uuid", 1, "PASSED",
                startedAt.plusSeconds(175),  // passedAt
                startedAt,                    // createdAt
                startedAt.plusSeconds(175),  // updatedAt
                2, "all tests passed");

        var timeline = builder.build(
                UUID.randomUUID(), startedAt, startedAt.plusSeconds(200),
                List.of(), List.of(), List.of(cp));

        assertThat(timeline.text()).contains("CHECKPOINT_SUBMIT: cp1 -> PASSED");
        assertThat(timeline.text()).contains("checkpoints_total: 1");
        assertThat(timeline.text()).contains("checkpoints_passed: 1");
    }

    @Test
    void mixedSources_areInterleavedByTimestamp() {
        var startedAt = Instant.parse("2026-04-07T10:00:00Z");
        var msg = new AiConversationView("USER", "first prompt",
                startedAt.plusSeconds(60));
        var event = new ActivityEventView(
                startedAt.plusSeconds(30), "file.opened",
                Map.of("filePath", "a.py"));

        var timeline = builder.build(
                UUID.randomUUID(), startedAt, startedAt.plusSeconds(120),
                List.of(msg), List.of(event), List.of());

        var text = timeline.text();
        int fileOpenedIdx = text.indexOf("FILE_OPENED");
        int candidateIdx = text.indexOf("CANDIDATE>");
        assertThat(fileOpenedIdx).isLessThan(candidateIdx);
    }

    @Test
    void longCandidateMessage_isTruncated() {
        var startedAt = Instant.parse("2026-04-07T10:00:00Z");
        var longContent = "x".repeat(800);
        var msg = new AiConversationView("USER", longContent,
                startedAt.plusSeconds(60));

        var timeline = builder.build(
                UUID.randomUUID(), startedAt, startedAt.plusSeconds(120),
                List.of(msg), List.of(), List.of());

        assertThat(timeline.text()).contains("[...truncated]");
    }
}
