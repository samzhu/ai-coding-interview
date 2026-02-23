package com.interview.interview.bdd;

import com.interview.TestcontainersConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, CapturedEvents.class, SharedInterviewState.class})
public class CucumberSpringContextConfiguration {
}
