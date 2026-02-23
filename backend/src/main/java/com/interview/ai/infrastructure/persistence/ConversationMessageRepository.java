package com.interview.ai.infrastructure.persistence;

import com.interview.ai.domain.ConversationMessage;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConversationMessageRepository extends ListCrudRepository<ConversationMessage, UUID> {

    @Query("SELECT * FROM ai_conversations WHERE interview_id = :interviewId ORDER BY created_at ASC")
    List<ConversationMessage> findByInterviewIdOrderByCreatedAtAsc(@Param("interviewId") UUID interviewId);
}
