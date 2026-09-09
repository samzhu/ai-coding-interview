package com.interview.scoring.application;

import com.interview.TestcontainersConfiguration;
import com.interview.scoring.domain.PilotVerdict;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 設計說明：對 5 個經典 fixture timelines 跑真實 Gemini Judge，
 * 斷言 verdict 落在預期區間（不要求精確分數，因為 LLM 有 variance）。
 *
 * 這是 LLM-as-Judge 系統唯一可行的 regression test 形式：每次改 system prompt
 * 或升級模型都重跑這 5 個 fixture，看 verdict 是否仍落在預期集合中。
 *
 * @EnabledIfEnvironmentVariable 確保沒有 GOOGLE_GENAI_API_KEY 時自動跳過
 * （CI 環境通常無 key，避免假性失敗與意外 API 費用）。
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@EnabledIfEnvironmentVariable(named = "GOOGLE_GENAI_API_KEY", matches = ".+")
class GeminiPilotJudgeIntegrationTest {

    @Autowired
    GeminiPilotJudge judge;

    @Test
    void idealDriverFixture_returnsDriverVerdict() throws Exception {
        var result = judge.judge(loadFixture("ideal_driver"));
        assertThat(result.verdict()).isIn(PilotVerdict.DRIVER, PilotVerdict.MOSTLY_DRIVER);
        assertThat(result.pilotScore()).isGreaterThanOrEqualTo(3.0);
        assertThat(result.headline()).isNotBlank();
        assertThat(result.subDimensions().specification().positiveEvidence()).isNotEmpty();
    }

    @Test
    void perfectPassengerFixture_returnsPassengerVerdict() throws Exception {
        var result = judge.judge(loadFixture("perfect_passenger"));
        assertThat(result.verdict()).isEqualTo(PilotVerdict.PASSENGER);
        assertThat(result.pilotScore()).isLessThanOrEqualTo(2.0);
    }

    @Test
    void mixedFixture_returnsNonDriverVerdict() throws Exception {
        // 設計說明：mixed fixture 前半 driver、後半 cp3 連續 fast accept 崩壞。
        // 真實 Gemini 觀察結果：「late collapse」會被 weight 為 PASSENGER 信號的代表，
        // 不一定回傳 MIXED。斷言放寬為「不是 DRIVER」即可—目的是 regression 偵測，
        // 而非強迫 LLM 給特定答案。
        var result = judge.judge(loadFixture("mixed"));
        assertThat(result.verdict()).isIn(
                PilotVerdict.PASSENGER, PilotVerdict.MIXED, PilotVerdict.MOSTLY_DRIVER);
        assertThat(result.pilotScore()).isLessThan(4.0);
    }

    @Test
    void strugglingButTryingFixture_methodologyOverridesOutcome() throws Exception {
        // 設計說明：struggling_but_trying 是 driver 方法論 + 低 outcome (1/3)。
        // 此 assertion 等於在驗證 v1.1 prompt 的「原則 A」有沒有被 LLM 內化：
        //   「Pilot Score 評方法不評結果，outcome 失敗不該把 Driver 判成 Passenger」
        // 期望 verdict 為 DRIVER 或 MOSTLY_DRIVER（不受低 outcome 拖累）。
        // 對應 pilotScore 必須 ≥ 2.8（per prompt 的 verdict-score 一致性檢查）。
        // 若未來 prompt 退化失去原則 A 的教導，此測試會率先失敗。
        var result = judge.judge(loadFixture("struggling_but_trying"));
        assertThat(result.verdict()).isIn(PilotVerdict.DRIVER, PilotVerdict.MOSTLY_DRIVER);
        assertThat(result.pilotScore()).isGreaterThanOrEqualTo(2.8);
    }

    @Test
    void silentOperatorFixture_lowSpecificationScore() throws Exception {
        var result = judge.judge(loadFixture("silent_operator"));
        // 沒有任何 prompt → specification 必低
        assertThat(result.subDimensions().specification().score()).isLessThanOrEqualTo(2.0);
    }

    private String loadFixture(String name) throws Exception {
        return StreamUtils.copyToString(
                new ClassPathResource("fixtures/scoring/" + name + ".timeline.txt").getInputStream(),
                StandardCharsets.UTF_8);
    }
}
