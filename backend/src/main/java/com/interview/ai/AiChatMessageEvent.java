package com.interview.ai;

import java.util.UUID;

public record AiChatMessageEvent(UUID interviewId, String role, String content) {
}
