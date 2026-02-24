package com.interview.question.interfaces.rest;

import com.interview.question.TestCaseDetail;

record TestCaseResponse(
        String id,
        String input,
        String expectedOutput) {

    static TestCaseResponse from(TestCaseDetail detail) {
        return new TestCaseResponse(detail.id(), detail.input(), detail.expectedOutput());
    }
}
