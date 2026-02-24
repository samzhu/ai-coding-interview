package com.interview.interview;

import java.util.UUID;

public record CodeSubmittedEvent(UUID interviewId, String checkpointId, int sequenceNumber, boolean passed) {
}
