package com.interview.monitoring;

import com.interview.monitoring.domain.ActivityEventRecord;
import com.interview.monitoring.infrastructure.ActivityEventJdbcRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityEventQueryServiceTest {

    @Mock ActivityEventJdbcRepository repository;
    @InjectMocks ActivityEventQueryService service;

    @Test
    void findByInterviewId_delegatesToRepository() {
        var id = UUID.randomUUID();
        var event = ActivityEventRecord.of(id, "file.opened", Map.of());
        when(repository.findByInterviewIdOrderByOccurredAt(id)).thenReturn(List.of(event));

        var result = service.findByInterviewId(id);

        assertThat(result).containsExactly(event);
    }
}
