package com.interview.interview;

import java.util.UUID;

public record CodeSubmittedEvent(UUID interviewId, UUID checkpointId, int sequenceNumber, boolean passed) {
}
