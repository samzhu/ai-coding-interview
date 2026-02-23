package com.interview.interview.interfaces.rest;

import java.util.Map;

public record SubmitCodeRequest(
        String code,
        Map<String, String> files) {
}
