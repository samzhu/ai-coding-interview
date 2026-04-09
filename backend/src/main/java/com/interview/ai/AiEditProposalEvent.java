package com.interview.ai;

import java.util.UUID;

/**
 * Published by ai module when AI proposes a code edit via the editProposal tool.
 * Subscribed by monitoring module for SSE broadcast and DB persistence,
 * and by scoring module's TimelineBuilder when reconstructing interview activity.
 *
 * 設計說明：Spring Modulith 跨模組公開 API 必須在模組根套件，不可在 internal/ 子套件。
 * record 而非 class，因為事件是不可變值物件。
 */
public record AiEditProposalEvent(
        UUID interviewId,
        String proposalId,
        String filePath,
        String diff
) {}
