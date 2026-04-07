package com.interview.scoring.infrastructure;

import com.interview.scoring.domain.InterviewScore;
import com.interview.scoring.domain.PilotJudgement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 設計說明：把 PilotJudgement 物件序列化成 JSON 字串再 save，
 * 避免 InterviewScore aggregate 自己持有 ObjectMapper 依賴。
 * Repository 只懂 String column。
 *
 * Use ObjectMapper from tools.jackson.databind (Spring Boot 4 / Jackson 3 path).
 * 若 @DataJdbcTest slice 不會自動注入 ObjectMapper，本 Saver 仍可在 production 取得，
 * 因為它是 @Component 走完整 Spring context；單元測試直接 new ObjectMapper() 即可。
 */
@Component
public class InterviewScoreSaver {

    private final InterviewScoreJdbcRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    InterviewScoreSaver(InterviewScoreJdbcRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public InterviewScore save(InterviewScore score, PilotJudgement judgement) {
        if (judgement != null) {
            try {
                score.setPilotJudgementJson(objectMapper.writeValueAsString(judgement));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize PilotJudgement", e);
            }
        }
        return repository.save(score);
    }

    public InterviewScore save(InterviewScore score) {
        return save(score, null);
    }
}
