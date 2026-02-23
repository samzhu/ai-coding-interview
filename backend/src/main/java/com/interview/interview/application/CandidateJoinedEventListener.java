package com.interview.interview.application;

import com.interview.invitation.CandidateJoinedEvent;
import com.interview.interview.domain.InterviewStatus;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class CandidateJoinedEventListener {

    private static final Logger log = LoggerFactory.getLogger(CandidateJoinedEventListener.class);

    private final InterviewService interviewService;
    private final InterviewRepository interviewRepository;

    CandidateJoinedEventListener(InterviewService interviewService,
                                  InterviewRepository interviewRepository) {
        this.interviewService = interviewService;
        this.interviewRepository = interviewRepository;
    }

    @EventListener
    public void onCandidateJoined(CandidateJoinedEvent event) {
        interviewRepository.findById(event.interviewId()).ifPresentOrElse(interview -> {
            if (interview.getInterviewStatus() == InterviewStatus.SCHEDULED) {
                interviewService.startInterview(event.interviewId());
            } else {
                log.info("Interview {} is already in status {}, skipping start",
                        event.interviewId(), interview.getStatus());
            }
        }, () -> log.warn("Interview {} not found for CandidateJoinedEvent", event.interviewId()));
    }
}
