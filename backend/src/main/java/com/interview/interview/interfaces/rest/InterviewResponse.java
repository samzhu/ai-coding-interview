package com.interview.interview.interfaces.rest;

import com.interview.interview.domain.Interview;

import java.time.Instant;
import java.util.UUID;

public record InterviewResponse(
        UUID id,
        UUID candidateId,
        UUID interviewerId,
        String title,
        String status,
        String questionId,
        String aiModel,
        int durationMinutes,
        Instant scheduledAt,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {

    public static InterviewResponse from(Interview interview) {
        return new InterviewResponse(
                interview.getId(),
                interview.getCandidateId(),
                interview.getInterviewerId(),
                interview.getTitle(),
                interview.getStatus(),
                interview.getQuestionId(),
                interview.getAiModel(),
                interview.getDurationMinutes(),
                interview.getScheduledAt(),
                interview.getStartedAt(),
                interview.getCompletedAt(),
                interview.getCreatedAt());
    }
}
