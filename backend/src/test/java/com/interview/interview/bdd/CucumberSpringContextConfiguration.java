package com.interview.interview.bdd;

import com.interview.TestcontainersConfiguration;
import com.interview.scoring.bdd.TestGeminiPilotJudgeConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({
        TestcontainersConfiguration.class,
        CapturedEvents.class,
        SharedInterviewState.class,
        TestCodeExecutorConfig.class,
        BddAsyncSyncConfig.class,
        TestGeminiPilotJudgeConfig.class
})
public class CucumberSpringContextConfiguration {
}
