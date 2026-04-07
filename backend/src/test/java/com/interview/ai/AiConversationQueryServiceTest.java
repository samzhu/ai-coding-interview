package com.interview.ai;

import com.interview.ai.domain.ConversationMessage;
import com.interview.ai.infrastructure.persistence.ConversationMessageRepository;
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
class AiConversationQueryServiceTest {

    @Mock
    ConversationMessageRepository repository;

    @InjectMocks
    AiConversationQueryService service;

    @Test
    void findByInterviewId_mapsToViewDto() {
        var id = UUID.randomUUID();
        var entity = mock(ConversationMessage.class);
        when(entity.getRole()).thenReturn("USER");
        when(entity.getContent()).thenReturn("hello");
        when(entity.getCreatedAt()).thenReturn(Instant.parse("2026-04-07T10:00:00Z"));

        when(repository.findByInterviewIdOrderByCreatedAtAsc(id)).thenReturn(List.of(entity));

        var result = service.findByInterviewId(id);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).role()).isEqualTo("USER");
        assertThat(result.get(0).content()).isEqualTo("hello");
        assertThat(result.get(0).createdAt()).isEqualTo(Instant.parse("2026-04-07T10:00:00Z"));
    }

    @Test
    void findByInterviewId_returnsEmptyList_whenNoMessages() {
        var id = UUID.randomUUID();
        when(repository.findByInterviewIdOrderByCreatedAtAsc(id)).thenReturn(List.of());

        var result = service.findByInterviewId(id);

        assertThat(result).isEmpty();
    }
}
