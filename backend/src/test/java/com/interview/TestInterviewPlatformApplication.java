package com.interview;

import org.springframework.boot.SpringApplication;

public class TestInterviewPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.from(InterviewPlatformApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
