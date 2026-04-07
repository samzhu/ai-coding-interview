package com.interview.interview;

import com.interview.interview.domain.CheckpointResult;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void findByInterviewId_delegatesToRepository() {
        var id = UUID.randomUUID();
        var checkpoint = mock(CheckpointResult.class);
        when(repository.findAllByInterviewId(id)).thenReturn(List.of(checkpoint));

        var result = service.findByInterviewId(id);

        assertThat(result).containsExactly(checkpoint);
    }

    @Test
    void findByInterviewId_returnsEmptyList_whenNoneExist() {
        var id = UUID.randomUUID();
        when(repository.findAllByInterviewId(id)).thenReturn(List.of());

        var result = service.findByInterviewId(id);

        assertThat(result).isEmpty();
    }
}
