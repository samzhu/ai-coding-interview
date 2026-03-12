package com.interview.invitation.application;

import com.interview.invitation.CandidateJoinedEvent;
import com.interview.invitation.InvitationInfo;
import com.interview.invitation.InvitationQueryService;
import com.interview.invitation.domain.Invitation;
import com.interview.invitation.infrastructure.persistence.InvitationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class InvitationService implements InvitationQueryService {

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

    /**
     * 實作 InvitationQueryService 公開介面。
     * 供 security 模組的 InvitationTokenAuthenticationFilter 跨模組查詢使用。
     * 只在 token 存在且未過期時回傳，確保過期 token 不通過驗證。
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<InvitationInfo> findValidInvitation(String token) {
        return repository.findByToken(token)
                .filter(inv -> !inv.isExpired())
                .map(inv -> new InvitationInfo(inv.getId(), inv.getInterviewId(), inv.getToken()));
    }
}
