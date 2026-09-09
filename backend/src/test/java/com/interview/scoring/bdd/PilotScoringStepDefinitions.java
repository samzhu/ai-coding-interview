package com.interview.scoring.bdd;

import com.interview.interview.InterviewCompletedEvent;
import com.interview.scoring.application.GeminiPilotJudge;
import com.interview.scoring.domain.InterviewScore;
import com.interview.scoring.domain.PilotJudgement;
import com.interview.scoring.domain.PilotVerdict;
import com.interview.scoring.domain.SubScore;
import com.interview.scoring.infrastructure.InterviewScoreJdbcRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.awaitility.Awaitility;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * BDD step definitions for Pilot Score 評分流程的端到端驗證。
 *
 * 設計說明：
 *  1. 雙軸驗證 — 每個 scenario 都同時檢查 outcome_score (規則) 與 pilot_verdict (LLM)，
 *     符合 spec §2 的 Hire 決策矩陣（hire 需雙軸都高）。
 *  2. Gemini Judge 由 TestGeminiPilotJudgeConfig 提供 Mockito mock；
 *     每個 scenario 在 @Given step 設定預期回應或例外，BDD 驗證 ScoringOrchestrator
 *     對三條路徑的處理：normal / failure / empty。
 *  3. 資料 seed 走 JdbcTemplate raw INSERT —— 避免依賴 InterviewService 等
 *     候選人應用層服務，BDD 焦點是 scoring 模組的行為。
 *  4. @Before 重設 mock 行為，避免 scenario 間互相污染。
 */
public class PilotScoringStepDefinitions {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private InterviewScoreJdbcRepository scoreRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private GeminiPilotJudge geminiPilotJudge;

    private UUID currentInterviewId;
    private InterviewScore latestScore;

    @Before("@pilot or not @pilot")
    public void resetState() {
        // 設計說明：mock 在 Cucumber Spring context 是 singleton，cross-scenario 必須重設，
        // 否則前一個 scenario 設的 when().thenReturn() 會延續到下一個。
        Mockito.reset(geminiPilotJudge);
        currentInterviewId = null;
        latestScore = null;
    }

    // ─────────────────────────────────────────────
    // Given: empty interview path (existing scenario)
    // ─────────────────────────────────────────────

    @Given("a completed interview with no AI conversations and no checkpoints")
    public void givenCompletedInterviewWithNoActivity() {
        currentInterviewId = UUID.randomUUID();
        seedInterviewRow(currentInterviewId);
    }

    // ─────────────────────────────────────────────
    // Given: full pipeline setup helpers
    // ─────────────────────────────────────────────

    @Given("a completed interview with the candidate")
    public void givenCompletedInterview() {
        currentInterviewId = UUID.randomUUID();
        seedInterviewRow(currentInterviewId);
    }

    @And("the candidate sent {int} USER prompts")
    public void candidateSentUserPrompts(int count) {
        for (int i = 0; i < count; i++) {
            jdbc.update("""
                    INSERT INTO ai_conversations (interview_id, role, content, created_at)
                    VALUES (?, 'USER', ?, NOW() + (interval '1 second' * ?))
                    """,
                    currentInterviewId,
                    "BDD-seeded prompt #" + (i + 1),
                    i);
        }
    }

    @And("the interview has {int} PASSED checkpoints out of {int}")
    public void seedCheckpoints(int passed, int total) {
        for (int i = 0; i < total; i++) {
            String status = i < passed ? "PASSED" : "FAILED";
            jdbc.update("""
                    INSERT INTO interview_checkpoint_results
                      (interview_id, checkpoint_id, checkpoint_sequence, status,
                       submission_count, version, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 1, 0, NOW(), NOW())
                    """,
                    currentInterviewId,
                    "cp-" + (i + 1),
                    i + 1,
                    status);
        }
    }

    @And("the interview has {int} PASSED checkpoints")
    public void seedCheckpointsAllPassed(int passed) {
        seedCheckpoints(passed, passed);
    }

    // ─────────────────────────────────────────────
    // Given: configure mocked GeminiPilotJudge
    // ─────────────────────────────────────────────

    @And("Gemini Judge will return verdict {string} with pilot score {double} and headline {string}")
    public void configureJudgeResponse(String verdictName, double score, String headline) {
        var verdict = PilotVerdict.valueOf(verdictName);
        var sub = new SubScore(score, "BDD stub rationale", List.of(), List.of());
        var judgement = new PilotJudgement(
                score, verdict,
                headline,
                "BDD stub summary for verdict " + verdictName,
                new PilotJudgement.SubDimensions(sub, sub, sub, sub));
        Mockito.when(geminiPilotJudge.judge(ArgumentMatchers.anyString())).thenReturn(judgement);
    }

    @And("Gemini Judge will throw {string}")
    public void configureJudgeException(String errorMessage) {
        Mockito.when(geminiPilotJudge.judge(ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException(errorMessage));
    }

    // ─────────────────────────────────────────────
    // When: publish completion event
    // ─────────────────────────────────────────────

    @When("the InterviewCompletedEvent is published")
    public void publishEvent() {
        // 設計說明：@ApplicationModuleListener 基於 @TransactionalEventListener(AFTER_COMMIT)，
        // 事件必須在 transaction commit 後才觸發。沒有圍繞 transaction 時直接 publish
        // 不會讓 listener 執行。TransactionTemplate 確保有 transaction 供 commit。
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new InterviewCompletedEvent(currentInterviewId));
            return null;
        });
    }

    // ─────────────────────────────────────────────
    // Then: assertions
    // ─────────────────────────────────────────────

    @Then("within 30 seconds an InterviewScore row exists for that interview")
    public void waitForScore() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<InterviewScore> score = scoreRepository.findByInterviewId(currentInterviewId);
                    assertThat(score).isPresent();
                });
        latestScore = scoreRepository.findByInterviewId(currentInterviewId).orElseThrow();
    }

    @And("the pilot_verdict is {string}")
    public void assertVerdict(String expected) {
        assertThat(latestScore.getPilotVerdict()).isEqualTo(expected);
    }

    @And("the outcome_score is {double}")
    public void assertOutcomeScore(double expected) {
        // 設計說明：outcome_score 是 NUMERIC(3,2) — DB 會 round 到小數點 2 位，
        // BDD 使用浮點數比較需給容差。
        assertThat(latestScore.getOutcomeScore()).isCloseTo(expected, within(0.01));
    }

    @And("the pilot_score is {double}")
    public void assertPilotScore(double expected) {
        assertThat(latestScore.getPilotScore()).isNotNull();
        assertThat(latestScore.getPilotScore()).isCloseTo(expected, within(0.01));
    }

    @And("the pilot_score is null")
    public void assertPilotScoreNull() {
        assertThat(latestScore.getPilotScore()).isNull();
    }

    @And("the pilot_headline contains {string}")
    public void assertHeadlineContains(String fragment) {
        assertThat(latestScore.getPilotHeadline()).contains(fragment);
    }

    @And("the judge_error_reason is null")
    public void assertNoJudgeError() {
        assertThat(latestScore.getJudgeErrorReason()).isNull();
    }

    @And("the judge_error_reason contains {string}")
    public void assertJudgeErrorContains(String fragment) {
        assertThat(latestScore.getJudgeErrorReason()).isNotNull();
        assertThat(latestScore.getJudgeErrorReason()).contains(fragment);
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private void seedInterviewRow(UUID interviewId) {
        // 直接 INSERT 最低限度的 interviews row，繞過 Application 層 ——
        // BDD 焦點是 scoring 模組的行為而非 interview 建立流程。
        // question_id 指向固定種子 UUID（migration 023 後該欄位為 VARCHAR）。
        jdbc.update("""
                INSERT INTO interviews
                  (id, candidate_id, interviewer_id, title, status, version,
                   question_id, scheduled_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'COMPLETED', 0,
                        '00000000-0000-0000-0000-000000000001', NOW(), NOW(), NOW())
                """,
                interviewId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BDD Pilot Score Test");
    }
}
