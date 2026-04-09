package com.interview.scoring.infrastructure;

import com.interview.scoring.domain.InterviewScore;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface InterviewScoreJdbcRepository extends CrudRepository<InterviewScore, UUID> {

    Optional<InterviewScore> findByInterviewId(UUID interviewId);

    void deleteByInterviewId(UUID interviewId);
}
