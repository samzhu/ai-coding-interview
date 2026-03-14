package com.interview.ai.infrastructure.persistence;

import com.interview.ai.domain.ConversationMessage;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMessageRepository extends ListCrudRepository<ConversationMessage, UUID> {

    @Query("SELECT * FROM ai_conversations WHERE interview_id = :interviewId ORDER BY created_at ASC")
    List<ConversationMessage> findByInterviewIdOrderByCreatedAtAsc(@Param("interviewId") UUID interviewId);

    @Modifying
    @Query("DELETE FROM ai_conversations WHERE interview_id = :interviewId")
    void deleteByInterviewId(@Param("interviewId") UUID interviewId);

    @Query("SELECT DISTINCT CAST(interview_id AS VARCHAR) FROM ai_conversations")
    List<String> findDistinctInterviewIds();

    @Query("SELECT * FROM ai_conversations WHERE interview_id = :interviewId AND role = 'ASSISTANT' ORDER BY created_at DESC LIMIT 1")
    Optional<ConversationMessage> findLastAssistantMessage(@Param("interviewId") UUID interviewId);
}
