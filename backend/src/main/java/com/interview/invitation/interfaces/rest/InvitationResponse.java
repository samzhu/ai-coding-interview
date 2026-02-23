package com.interview.invitation.interfaces.rest;

import com.interview.invitation.domain.Invitation;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID interviewId,
        String token,
        Instant expiresAt,
        Instant createdAt) {

    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getInterviewId(),
                invitation.getToken(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt());
    }
}
