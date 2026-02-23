package com.interview.invitation;

import java.util.UUID;

public record CandidateJoinedEvent(UUID interviewId, UUID invitationId) {
}
