package com.interview.interview;

import com.interview.interview.domain.CheckpointResult;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 設計說明：scoring 模組計算 Outcome 軸與重建 timeline 時讀取 checkpoint 結果。
 * 透過此根套件 service 取得，避免跨模組直接 inject 內層 Repository。
 * 回傳 CheckpointResultView (DTO) 而非 domain entity，否則 Spring Modulith 會
 * 報 MODULITH_TYPE_REF_VIOLATION (domain 子套件預設為內部型別)。
 */
@Service
public class CheckpointResultQueryService {

    private final CheckpointResultRepository repository;

    CheckpointResultQueryService(CheckpointResultRepository repository) {
        this.repository = repository;
    }

    public List<CheckpointResultView> findByInterviewId(UUID interviewId) {
        return repository.findAllByInterviewId(interviewId).stream()
                .map(CheckpointResultQueryService::toView)
                .toList();
    }

    private static CheckpointResultView toView(CheckpointResult entity) {
        return new CheckpointResultView(
                entity.getCheckpointId(),
                entity.getCheckpointSequence(),
                entity.getStatus(),
                entity.getPassedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getSubmissionCount(),
                entity.getExecutionOutput()
        );
    }
}
