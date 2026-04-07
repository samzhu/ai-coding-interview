package com.interview.interview;

import com.interview.interview.domain.CheckpointResult;
import com.interview.interview.infrastructure.persistence.CheckpointResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 設計說明：scoring 模組計算 Outcome 軸時讀取 checkpoint 結果，
 * 透過此根套件 service 取得，避免跨模組直接 inject 內層 Repository。
 * CheckpointResult 為 domain entity（非 internal infrastructure），可安全跨模組暴露。
 */
@Service
public class CheckpointResultQueryService {

    private final CheckpointResultRepository repository;

    CheckpointResultQueryService(CheckpointResultRepository repository) {
        this.repository = repository;
    }

    public List<CheckpointResult> findByInterviewId(UUID interviewId) {
        return repository.findAllByInterviewId(interviewId);
    }
}
