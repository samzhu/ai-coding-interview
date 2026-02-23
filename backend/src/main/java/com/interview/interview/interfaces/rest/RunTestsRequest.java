package com.interview.interview.interfaces.rest;

import java.util.Map;

public record RunTestsRequest(
        Map<String, String> files) {
}
