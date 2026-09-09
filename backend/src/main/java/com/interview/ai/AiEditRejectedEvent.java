package com.interview.ai;

import java.util.UUID;

/**
 * Published when the candidate rejects an AI edit proposal.
 * Any rejection event is a strong positive signal for Pilot Score —
 * it indicates the candidate is reviewing AI output critically.
 */
public record AiEditRejectedEvent(UUID interviewId, String proposalId) {}
