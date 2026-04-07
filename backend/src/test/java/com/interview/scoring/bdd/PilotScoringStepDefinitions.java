package com.interview.scoring.bdd;

import com.interview.interview.InterviewCompletedEvent;
import com.interview.scoring.domain.InterviewScore;
import com.interview.scoring.infrastructure.InterviewScoreJdbcRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 設計說明：覆蓋零互動早退路徑（conversations.isEmpty() && checkpoints.isEmpty()），
 * 這條路徑不呼叫 Gemini，直接寫入 PASSENGER verdict。
 *
 * 選擇此路徑的原因：
 * 1. 不需要 mock GeminiPilotJudge bean，Spring context 保持原樣
 * 2. 早退邏輯本身是重要業務規則，值得 BDD 覆蓋
 * 3. Awaitility 輪詢 @ApplicationModuleListener 的非同步寫入，避免 Thread.sleep 硬等
 *
 * 若未來需要覆蓋 LLM 路徑，可用 @Primary @MockitoBean 替換 GeminiPilotJudge，
 * 但需在 CucumberSpringContextConfiguration 中額外 Import。
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

    private UUID currentInterviewId;
    private InterviewScore latestScore;

    @Given("a completed interview with no AI conversations and no checkpoints")
    public void givenCompletedInterviewWithNoActivity() {
        currentInterviewId = UUID.randomUUID();
        // 直接 INSERT 最低限度的 interviews 列，繞過 Application 層，
        // 因為 BDD 此處驗證的是 scoring 模組行為而非 interview 建立流程。
        // question_id 指向固定種子 UUID，符合 Liquibase 005 遷移後的 schema。
        jdbc.update("""
                INSERT INTO interviews
                  (id, candidate_id, interviewer_id, title, status, version,
                   question_id, scheduled_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'COMPLETED', 0,
                        '00000000-0000-0000-0000-000000000001', NOW(), NOW(), NOW())
                """,
                currentInterviewId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BDD Scoring Test"
        );
    }

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

    @Then("the pilot_verdict is {string}")
    public void assertVerdict(String expected) {
        assertThat(latestScore.getPilotVerdict()).isEqualTo(expected);
    }

    @Then("the outcome_score is {double}")
    public void assertOutcomeScore(double expected) {
        assertThat(latestScore.getOutcomeScore()).isEqualTo(expected);
    }

    @Then("the judge_error_reason is null")
    public void assertNoJudgeError() {
        assertThat(latestScore.getJudgeErrorReason()).isNull();
    }
}
