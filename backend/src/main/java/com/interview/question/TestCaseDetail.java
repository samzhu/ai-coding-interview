package com.interview.question;

public record TestCaseDetail(
        String id,
        String input,
        String expectedOutput,
        boolean isHidden) {
}
