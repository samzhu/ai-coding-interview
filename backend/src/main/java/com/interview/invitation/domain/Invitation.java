package com.interview.invitation.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Table("interview_invitations")
public class Invitation {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column("interview_id")
    private UUID interviewId;

    private String token;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("created_at")
    private Instant createdAt;

    protected Invitation() {
    }

    public static Invitation create(UUID interviewId) {
        Invitation invitation = new Invitation();
        invitation.id = UUID.randomUUID();
        invitation.interviewId = interviewId;
        invitation.token = generateToken();
        invitation.expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        invitation.createdAt = Instant.now();
        return invitation;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void validateForJoin() {
        if (isExpired()) {
            throw new IllegalStateException("Invitation has expired");
        }
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public UUID getId() { return id; }
    public UUID getInterviewId() { return interviewId; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
