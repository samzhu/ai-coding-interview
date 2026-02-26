package com.interview.ai.interfaces.rest;

import java.util.List;

record StreamChatRequest(String id, List<AiSdkMessage> messages, String modelId) {

    record AiSdkMessage(String id, String role, List<AiSdkPart> parts) {}

    record AiSdkPart(String type, String text) {}

    String extractLastUserMessage() {
        if (messages == null || messages.isEmpty()) return "";
        return messages.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b)
                .flatMap(m -> m.parts().stream()
                        .filter(p -> "text".equals(p.type()) && p.text() != null)
                        .map(AiSdkPart::text)
                        .findFirst())
                .orElse("");
    }
}
