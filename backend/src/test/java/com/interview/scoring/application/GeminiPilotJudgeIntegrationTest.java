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
    void mixedFixture_returnsMixedOrMostlyDriver() throws Exception {
        var result = judge.judge(loadFixture("mixed"));
        assertThat(result.verdict()).isIn(PilotVerdict.MIXED, PilotVerdict.MOSTLY_DRIVER);
    }

    @Test
    void strugglingButTryingFixture_pilotScoreHigherThanOutcome() throws Exception {
        var result = judge.judge(loadFixture("struggling_but_trying"));
        assertThat(result.verdict()).isIn(PilotVerdict.MOSTLY_DRIVER, PilotVerdict.MIXED);
        assertThat(result.pilotScore()).isGreaterThanOrEqualTo(2.5);
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
