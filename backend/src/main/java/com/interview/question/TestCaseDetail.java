package com.interview.question;

import java.util.UUID;

public record TestCaseDetail(
        UUID id,
        String input,
        String expectedOutput,
        boolean isHidden) {
}
