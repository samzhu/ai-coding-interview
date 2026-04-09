package com.interview.ai;

import java.util.UUID;

/**
 * Published when the candidate accepts an AI edit proposal.
 * Pairs with AiEditProposalEvent via proposalId; the time delta between
 * the two events is the candidate's "review time" — a key signal for
 * Pilot Score's Critical Review dimension.
 */
public record AiEditAcceptedEvent(UUID interviewId, String proposalId) {}
