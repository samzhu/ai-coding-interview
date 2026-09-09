package com.interview.monitoring.application;

import com.interview.ai.AiEditAcceptedEvent;
import com.interview.ai.AiEditProposalEvent;
import com.interview.ai.AiEditRejectedEvent;
import com.interview.monitoring.domain.ActivityEventRecord;
import com.interview.monitoring.infrastructure.ActivityEventJdbcRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonitoringEventPersisterTest {

    @Mock ActivityEventJdbcRepository repository;
    @InjectMocks MonitoringEventPersister persister;

    @Test
    void onAiEditProposal_persistsWithDiffPayload() {
        var interviewId = UUID.randomUUID();
        var event = new AiEditProposalEvent(interviewId, "p-001", "solution.py", "@@ diff @@");

        persister.onAiEditProposal(event);

        var captor = ArgumentCaptor.forClass(ActivityEventRecord.class);
        verify(repository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getInterviewId()).isEqualTo(interviewId);
        assertThat(saved.getEventType()).isEqualTo("ai.edit.proposal");
        assertThat(saved.getPayload())
                .containsEntry("proposalId", "p-001")
                .containsEntry("filePath", "solution.py")
                .containsEntry("diff", "@@ diff @@");
    }

    @Test
    void onAiEditAccepted_persistsWithProposalIdOnly() {
        var event = new AiEditAcceptedEvent(UUID.randomUUID(), "p-001");
        persister.onAiEditAccepted(event);
        var captor = ArgumentCaptor.forClass(ActivityEventRecord.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ai.edit.accepted");
        assertThat(captor.getValue().getPayload()).containsEntry("proposalId", "p-001");
    }

    @Test
    void onAiEditRejected_persists() {
        var event = new AiEditRejectedEvent(UUID.randomUUID(), "p-001");
        persister.onAiEditRejected(event);
        var captor = ArgumentCaptor.forClass(ActivityEventRecord.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ai.edit.rejected");
    }
}
