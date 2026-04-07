package com.interview.monitoring;

import com.interview.monitoring.domain.ActivityEventRecord;
import com.interview.monitoring.infrastructure.ActivityEventJdbcRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Public read API exposed at module root, callable by other modules (scoring).
 *
 * 設計說明：跨模組查詢必須走根套件的 Service，不可直接 inject 內層 Repository
 * (Spring Modulith 邊界由 ModularityTests 強制執行)。
 */
@Service
public class ActivityEventQueryService {

    private final ActivityEventJdbcRepository repository;

    ActivityEventQueryService(ActivityEventJdbcRepository repository) {
        this.repository = repository;
    }

    public List<ActivityEventRecord> findByInterviewId(UUID interviewId) {
        return repository.findByInterviewIdOrderByOccurredAt(interviewId);
    }
}
