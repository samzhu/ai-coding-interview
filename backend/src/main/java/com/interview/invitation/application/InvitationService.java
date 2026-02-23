package com.interview.invitation.application;

import com.interview.invitation.CandidateJoinedEvent;
import com.interview.invitation.domain.Invitation;
import com.interview.invitation.infrastructure.persistence.InvitationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class InvitationService {

    private final InvitationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public InvitationService(InvitationRepository repository,
                              ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public Invitation createOrGetInvitation(UUID interviewId) {
        return repository.findByInterviewId(interviewId)
                .orElseGet(() -> repository.save(Invitation.create(interviewId)));
    }

    @Transactional(readOnly = true)
    public Invitation getByToken(String token) {
        return repository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found for token"));
    }

    public Invitation joinByToken(String token) {
        Invitation invitation = repository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found for token"));
        invitation.validateForJoin();
        eventPublisher.publishEvent(new CandidateJoinedEvent(invitation.getInterviewId(), invitation.getId()));
        return invitation;
    }
}
