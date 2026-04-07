package com.interview.ai;

import com.interview.ai.domain.ConversationMessage;
import com.interview.ai.infrastructure.persistence.ConversationMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 設計說明：scoring 模組透過此根套件 API 讀取對話記錄重建 timeline。
 * 回傳 AiConversationView (DTO) 而非內部 entity，維持 ai 模組封裝邊界。
 */
@Service
public class AiConversationQueryService {

    private final ConversationMessageRepository repository;

    AiConversationQueryService(ConversationMessageRepository repository) {
        this.repository = repository;
    }

    public List<AiConversationView> findByInterviewId(UUID interviewId) {
        return repository.findByInterviewIdOrderByCreatedAtAsc(interviewId).stream()
                .map(this::toView)
                .toList();
    }

    private AiConversationView toView(ConversationMessage message) {
        return new AiConversationView(
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
