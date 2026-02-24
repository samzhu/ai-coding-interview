package com.interview.interview.infrastructure.persistence;

import com.interview.interview.domain.CheckpointResult;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckpointResultRepository extends ListCrudRepository<CheckpointResult, UUID> {

    @Query("SELECT * FROM interview_checkpoint_results WHERE interview_id = :interviewId ORDER BY checkpoint_sequence ASC")
    List<CheckpointResult> findAllByInterviewId(@Param("interviewId") UUID interviewId);

    @Query("SELECT * FROM interview_checkpoint_results WHERE interview_id = :interviewId AND checkpoint_id = :checkpointId")
    Optional<CheckpointResult> findByInterviewIdAndCheckpointId(
            @Param("interviewId") UUID interviewId,
            @Param("checkpointId") String checkpointId);

    @Query("SELECT * FROM interview_checkpoint_results WHERE interview_id = :interviewId AND status != 'PASSED' ORDER BY checkpoint_sequence ASC LIMIT 1")
    Optional<CheckpointResult> findCurrentCheckpoint(@Param("interviewId") UUID interviewId);
}
