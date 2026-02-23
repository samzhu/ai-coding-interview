package com.interview.invitation.application;

import com.interview.invitation.CandidateJoinedEvent;
import com.interview.invitation.domain.Invitation;
import com.interview.invitation.infrastructure.persistence.InvitationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationService 業務邏輯")
class InvitationServiceTest {

    @Mock
    private InvitationRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InvitationService service;

    @Test
    @DisplayName("建立邀請 — 無舊邀請時應建立新的")
    void shouldCreateNewInvitationWhenNoneExists() {
        UUID interviewId = UUID.randomUUID();
        Invitation created = Invitation.create(interviewId);
        when(repository.findByInterviewId(interviewId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(created);

        Invitation result = service.createOrGetInvitation(interviewId);

        assertThat(result.getInterviewId()).isEqualTo(interviewId);
        assertThat(result.getToken()).isNotBlank();
        verify(repository).save(any(Invitation.class));
    }

    @Test
    @DisplayName("建立邀請 — 已有邀請時應回傳舊的（冪等）")
    void shouldReturnExistingInvitationWhenAlreadyExists() {
        UUID interviewId = UUID.randomUUID();
        Invitation existing = Invitation.create(interviewId);
        when(repository.findByInterviewId(interviewId)).thenReturn(Optional.of(existing));

        Invitation result = service.createOrGetInvitation(interviewId);

        assertThat(result.getToken()).isEqualTo(existing.getToken());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("候選人加入 — 應發布 CandidateJoinedEvent")
    void shouldPublishCandidateJoinedEventOnJoin() {
        UUID interviewId = UUID.randomUUID();
        Invitation invitation = Invitation.create(interviewId);
        when(repository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));

        service.joinByToken(invitation.getToken());

        ArgumentCaptor<CandidateJoinedEvent> captor = ArgumentCaptor.forClass(CandidateJoinedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().interviewId()).isEqualTo(interviewId);
    }

    @Test
    @DisplayName("使用不存在的 token 加入應拋出例外")
    void shouldThrowWhenTokenNotFound() {
        when(repository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.joinByToken("invalid-token"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
