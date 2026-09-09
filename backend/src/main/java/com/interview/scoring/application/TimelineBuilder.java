package com.interview.scoring.application;

import com.interview.ai.AiConversationView;
import com.interview.interview.CheckpointResultView;
import com.interview.monitoring.ActivityEventView;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * 設計說明：TimelineBuilder 的職責是把三個獨立資料來源（AI 對話、前端活動事件、Checkpoint
 * 執行結果）合併成一條以時間排序的 timeline 字串，供 Gemini judge 評分。
 *
 * 三個資料來源各有獨立時間戳，合併後依照 occurredAt/createdAt 排序，保證因果關係
 * 與 Gemini 判斷的一致性。
 *
 * review_seconds 的計算方式：對每一個 ai.edit.accepted 事件，先以 proposalId 向前
 * 找最近的 ai.edit.proposal，再算兩者之間的秒差。這個 join 在合併排序前先以 Map
 * 建立索引，避免 O(n²) 掃描。
 *
 * 為了讓 Gemini 能準確評分，CANDIDATE/AI 訊息超過 500 字元的部分會被截斷；
 * 題目通常包含重複的框架碼，前 500 字元已足以表達意圖。
 */
@Component
public class TimelineBuilder {

    private static final int MAX_MESSAGE_CHARS = 500;
    private static final int FAST_ACCEPT_THRESHOLD_SECONDS = 5;

    // TODO(Task 19 follow-up): if approxTokenCount > 30_000, apply sliding window
    // compression per spec §3.2 — preserve first 10% + last 30%, compress middle.
    // Deferred from MVP because typical 60-min interview produces 5-25k tokens.

    public Timeline build(
            UUID interviewId,
            Instant startedAt,
            Instant endedAt,
            List<AiConversationView> conversations,
            List<ActivityEventView> activityEvents,
            List<CheckpointResultView> checkpoints) {

        var sb = new StringBuilder();
        appendHeader(sb, startedAt, endedAt, checkpoints);
        appendEvents(sb, startedAt, endedAt, conversations, activityEvents, checkpoints);
        appendStats(sb, conversations, activityEvents, checkpoints);

        var text = sb.toString();
        return new Timeline(text, text.length() / 4);
    }

    private void appendHeader(StringBuilder sb, Instant startedAt, Instant endedAt,
                              List<CheckpointResultView> checkpoints) {
        long passedCount = checkpoints.stream()
                .filter(cp -> "PASSED".equals(cp.status()))
                .count();

        sb.append("=== INTERVIEW TIMELINE ===\n");
        sb.append("duration: ").append(formatDuration(Duration.between(startedAt, endedAt))).append("\n");
        sb.append("checkpoints_total: ").append(checkpoints.size()).append("\n");
        sb.append("checkpoints_passed: ").append(passedCount).append("\n");
        sb.append("\n");
    }

    private void appendEvents(StringBuilder sb, Instant startedAt, Instant endedAt,
                              List<AiConversationView> conversations,
                              List<ActivityEventView> activityEvents,
                              List<CheckpointResultView> checkpoints) {
        // Pre-compute proposal times by proposalId for review_seconds join.
        // A proposal may appear multiple times; we keep only the most recent one
        // before the accept, so we index as we iterate in time order.
        Map<String, Instant> latestProposalTimeByProposalId = buildProposalIndex(activityEvents);

        List<TimelineEntry> entries = new ArrayList<>();

        // Anchor events
        entries.add(new TimelineEntry(startedAt, "INTERVIEW_START"));
        entries.add(new TimelineEntry(endedAt, "INTERVIEW_END"));

        // AI conversation messages
        for (AiConversationView msg : conversations) {
            String line = buildConversationLine(msg);
            if (line != null) {
                entries.add(new TimelineEntry(msg.createdAt(), line));
            }
        }

        // Activity events
        for (ActivityEventView event : activityEvents) {
            String line = buildActivityLine(event, latestProposalTimeByProposalId);
            if (line != null) {
                entries.add(new TimelineEntry(event.occurredAt(), line));
            }
        }

        // Checkpoint results — use passedAt for PASSED, updatedAt otherwise.
        // Skip checkpoints still in PENDING or IN_PROGRESS (no submission yet).
        for (CheckpointResultView cp : checkpoints) {
            if ("PENDING".equals(cp.status()) || "IN_PROGRESS".equals(cp.status())) {
                continue;
            }
            Instant eventTime = "PASSED".equals(cp.status()) ? cp.passedAt() : cp.updatedAt();
            if (eventTime == null) {
                eventTime = cp.updatedAt();
            }
            entries.add(new TimelineEntry(eventTime,
                    "CHECKPOINT_SUBMIT: cp" + cp.checkpointSequence() + " -> " + cp.status()));
        }

        // Sort by time; stable sort preserves insertion order for same-millisecond events.
        entries.sort(Comparator.comparing(TimelineEntry::time));

        for (TimelineEntry entry : entries) {
            sb.append("[")
              .append(formatOffset(Duration.between(startedAt, entry.time())))
              .append("] ")
              .append(entry.line())
              .append("\n");
        }
        sb.append("\n");
    }

    /**
     * Builds a proposalId → Instant index from ai.edit.proposal events, sorted by time.
     * When the same proposalId appears more than once, the latest occurrence wins —
     * this handles edge cases where a proposal is re-emitted before acceptance.
     */
    private Map<String, Instant> buildProposalIndex(List<ActivityEventView> activityEvents) {
        Map<String, Instant> index = new HashMap<>();
        activityEvents.stream()
                .filter(e -> "ai.edit.proposal".equals(e.eventType()))
                .sorted(Comparator.comparing(ActivityEventView::occurredAt))
                .forEach(e -> {
                    String proposalId = payloadString(e.payload(), "proposalId");
                    if (proposalId != null) {
                        index.put(proposalId, e.occurredAt());
                    }
                });
        return index;
    }

    private String buildConversationLine(AiConversationView msg) {
        String content = truncate(msg.content());
        return switch (msg.role()) {
            case "USER" -> "CANDIDATE> \"" + content + "\"";
            case "ASSISTANT" -> "AI> \"" + content + "\"";
            default -> null; // SYSTEM messages are not shown in timeline
        };
    }

    private String buildActivityLine(ActivityEventView event,
                                     Map<String, Instant> proposalIndex) {
        Map<String, Object> p = event.payload();
        return switch (event.eventType()) {
            case "ai.edit.proposal" -> {
                String filePath = payloadString(p, "filePath");
                yield "AI_PROPOSE: " + (filePath != null ? filePath : "unknown");
            }
            case "ai.edit.accepted" -> {
                String proposalId = payloadString(p, "proposalId");
                Instant proposalTime = (proposalId != null) ? proposalIndex.get(proposalId) : null;
                StringBuilder line = new StringBuilder("AI_ACCEPT: ");
                if (proposalId != null) {
                    line.append(proposalId);
                }
                if (proposalTime != null) {
                    long reviewSeconds = Duration.between(proposalTime, event.occurredAt()).toSeconds();
                    line.append("    review_seconds=").append(reviewSeconds);
                    if (reviewSeconds < FAST_ACCEPT_THRESHOLD_SECONDS) {
                        line.append("   <FAST>");
                    }
                }
                yield line.toString();
            }
            case "ai.edit.rejected" -> {
                String proposalId = payloadString(p, "proposalId");
                yield "AI_REJECT: " + (proposalId != null ? proposalId : "unknown");
            }
            case "file.opened" -> "FILE_OPENED: " + payloadString(p, "filePath");
            case "file.closed" -> "FILE_CLOSED: " + payloadString(p, "filePath");
            case "terminal.test.run" -> "TERMINAL: " + payloadString(p, "command");
            case "code.manual_edit" -> {
                String filePath = payloadString(p, "filePath");
                Object charDelta = p.get("charDelta");
                yield "MANUAL_EDIT: " + filePath + " (Δ" + charDelta + ")";
            }
            default -> null; // unknown event types are silently skipped
        };
    }

    private void appendStats(StringBuilder sb, List<AiConversationView> conversations,
                             List<ActivityEventView> activityEvents,
                             List<CheckpointResultView> checkpoints) {
        // Compute accept→proposal review_seconds to identify fast accepts.
        Map<String, Instant> proposalIndex = buildProposalIndex(activityEvents);

        long totalPrompts = conversations.stream()
                .filter(m -> "USER".equals(m.role())).count();
        long totalAssistantMsgs = conversations.stream()
                .filter(m -> "ASSISTANT".equals(m.role())).count();
        long totalProposals = activityEvents.stream()
                .filter(e -> "ai.edit.proposal".equals(e.eventType())).count();
        long totalAccepts = activityEvents.stream()
                .filter(e -> "ai.edit.accepted".equals(e.eventType())).count();
        long totalRejects = activityEvents.stream()
                .filter(e -> "ai.edit.rejected".equals(e.eventType())).count();
        long manualEdits = activityEvents.stream()
                .filter(e -> "code.manual_edit".equals(e.eventType())).count();
        long testRuns = activityEvents.stream()
                .filter(e -> "terminal.test.run".equals(e.eventType())).count();
        long fastAccepts = activityEvents.stream()
                .filter(e -> "ai.edit.accepted".equals(e.eventType()))
                .filter(e -> {
                    String proposalId = payloadString(e.payload(), "proposalId");
                    Instant proposalTime = (proposalId != null) ? proposalIndex.get(proposalId) : null;
                    if (proposalTime == null) return false;
                    return Duration.between(proposalTime, e.occurredAt()).toSeconds() < FAST_ACCEPT_THRESHOLD_SECONDS;
                })
                .count();
        long filesOpened = activityEvents.stream()
                .filter(e -> "file.opened".equals(e.eventType())).count();
        long checkpointSubmits = checkpoints.stream()
                .filter(cp -> !"PENDING".equals(cp.status()) && !"IN_PROGRESS".equals(cp.status()))
                .count();

        sb.append("=== AGGREGATE STATS ===\n");
        sb.append(statLine("total_prompts:", totalPrompts));
        sb.append(statLine("total_assistant_msgs:", totalAssistantMsgs));
        sb.append(statLine("total_proposals:", totalProposals));
        sb.append(statLine("total_accepts:", totalAccepts));
        sb.append(statLine("total_rejects:", totalRejects));
        sb.append(statLine("manual_edits:", manualEdits));
        sb.append(statLine("test_runs:", testRuns));
        sb.append(statLine("fast_accepts (<5s):", fastAccepts));
        sb.append(statLine("files_opened:", filesOpened));
        sb.append(statLine("checkpoint_submits:", checkpointSubmits));
    }

    // Left-justifies the label in a 25-char field so values align cleanly for the LLM.
    private String statLine(String label, long value) {
        return String.format("%-25s%d\n", label, value);
    }

    private String truncate(String content) {
        if (content == null) return "";
        if (content.length() <= MAX_MESSAGE_CHARS) return content;
        return content.substring(0, MAX_MESSAGE_CHARS) + " [...truncated]";
    }

    /**
     * Extracts a string value from the payload map.
     * ActivityEventView payload values may be deserialized as String or other types
     * depending on the JSON source; toString() handles both cases safely.
     */
    private String payloadString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }

    /** Formats a Duration as mm:ss (no hours component — most interviews are < 60 min). */
    private String formatOffset(Duration d) {
        long totalSeconds = Math.max(0, d.toSeconds());
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /** Formats total interview duration. Same mm:ss format for consistency. */
    private String formatDuration(Duration d) {
        long totalSeconds = Math.max(0, d.toSeconds());
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private record TimelineEntry(Instant time, String line) {}

    public record Timeline(String text, int approxTokenCount) {}
}
