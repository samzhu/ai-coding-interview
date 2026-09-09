package com.interview.interview;

import com.interview.interview.domain.CheckpointResult;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckpointResultQueryServiceTest {

    @Mock
    CheckpointResultRepository repository;

    @InjectMocks
    CheckpointResultQueryService service;

    @Test
    void findByInterviewId_mapsEntitiesToViews() {
        var interviewId = UUID.randomUUID();
        var entity = mock(CheckpointResult.class);
        when(entity.getCheckpointId()).thenReturn("cp-1");
        when(entity.getCheckpointSequence()).thenReturn(1);
        when(entity.getStatus()).thenReturn("PASSED");
        when(entity.getPassedAt()).thenReturn(Instant.parse("2026-04-07T10:00:00Z"));
        when(entity.getCreatedAt()).thenReturn(Instant.parse("2026-04-07T09:55:00Z"));
        when(entity.getUpdatedAt()).thenReturn(Instant.parse("2026-04-07T10:00:00Z"));
        when(entity.getSubmissionCount()).thenReturn(2);
        when(entity.getExecutionOutput()).thenReturn("ok");
        when(repository.findAllByInterviewId(interviewId)).thenReturn(List.of(entity));

        var result = service.findByInterviewId(interviewId);

        assertThat(result).hasSize(1);
        var view = result.get(0);
        assertThat(view.checkpointId()).isEqualTo("cp-1");
        assertThat(view.status()).isEqualTo("PASSED");
        assertThat(view.submissionCount()).isEqualTo(2);
        assertThat(view.executionOutput()).isEqualTo("ok");
    }

    @Test
    void findByInterviewId_returnsEmptyList_whenNoneExist() {
        var id = UUID.randomUUID();
        when(repository.findAllByInterviewId(id)).thenReturn(List.of());

        var result = service.findByInterviewId(id);

        assertThat(result).isEmpty();
    }
}
