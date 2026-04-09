package com.interview.scoring.bdd;

import com.interview.scoring.application.GeminiPilotJudge;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * 設計說明：BDD 不能也不應該打真實 Gemini API（成本 + 不確定性）。
 *
 * 用 Mockito.mock(GeminiPilotJudge.class) 提供 @Primary bean 蓋過 production 的
 * GeminiPilotJudge，step definitions autowire 後用 Mockito.when(...) 設定每個
 * scenario 的回應或例外。Mockito 可以 mock 具體類別（不需要 interface），且
 * 不會呼叫原本的 constructor，所以不需要準備 AiChatClientLookup 等依賴。
 *
 * 透過 @Import 載入到 CucumberSpringContextConfiguration，僅在 BDD context 生效；
 * production 與其他 SpringBootTest 仍使用真實的 GeminiPilotJudge bean。
 */
@TestConfiguration
public class TestGeminiPilotJudgeConfig {

    @Bean
    @Primary
    GeminiPilotJudge testGeminiPilotJudge() {
        return mock(GeminiPilotJudge.class);
    }
}
