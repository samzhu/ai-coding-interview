package com.interview.interview;

import com.interview.interview.domain.Interview;
import com.interview.interview.domain.InterviewStatus;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * 跨模組公開 API：查詢面試時間狀態。
 * ai 等模組透過此介面判斷面試是否已超時。
 */
@Service
public class InterviewTimeProvider {

    private final InterviewRepository repository;

    public InterviewTimeProvider(InterviewRepository repository) {
        this.repository = repository;
    }

    /**
     * 判斷指定面試是否已超時。
     * 面試未開始或非 IN_PROGRESS 狀態回傳 false。
     */
    @Transactional(readOnly = true)
    public boolean isExpired(UUID interviewId) {
        return repository.findById(interviewId)
                .map(this::checkExpired)
                .orElse(false);
    }

    /**
     * 取得剩餘秒數，若面試未開始或不存在則回傳 empty。
     */
    @Transactional(readOnly = true)
    public OptionalLong getRemainingSeconds(UUID interviewId) {
        return repository.findById(interviewId)
                .filter(i -> i.getInterviewStatus() == InterviewStatus.IN_PROGRESS)
                .filter(i -> i.getStartedAt() != null)
                .map(i -> {
                    long totalSeconds = (long) i.getDurationMinutes() * 60;
                    long elapsed = Instant.now().getEpochSecond() - i.getStartedAt().getEpochSecond();
                    return Math.max(0L, totalSeconds - elapsed);
                })
                .map(OptionalLong::of)
                .orElse(OptionalLong.empty());
    }

    private boolean checkExpired(Interview interview) {
        if (interview.getInterviewStatus() != InterviewStatus.IN_PROGRESS) return false;
        if (interview.getStartedAt() == null) return false;
        long totalSeconds = (long) interview.getDurationMinutes() * 60;
        long elapsedSeconds = Instant.now().getEpochSecond() - interview.getStartedAt().getEpochSecond();
        return elapsedSeconds >= totalSeconds;
    }
}
