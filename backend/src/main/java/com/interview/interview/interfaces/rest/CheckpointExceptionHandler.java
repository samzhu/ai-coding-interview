package com.interview.interview.interfaces.rest;

import com.interview.interview.InterviewExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CheckpointController.class)
class CheckpointExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CheckpointExceptionHandler.class);

    @ExceptionHandler(InterviewExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    ProblemDetail handleExpired(InterviewExpiredException ex) {
        log.warn("Checkpoint request rejected (expired): {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        log.warn("Checkpoint request rejected (bad request): {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleConflict(IllegalStateException ex) {
        log.warn("Checkpoint request rejected (conflict): {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * 設計說明：捕捉所有未預期例外（如容器操作失敗），記錄完整 stack trace 以便診斷，
     * 並返回不暴露內部細節的 ProblemDetail，避免資訊洩漏。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Checkpoint request failed (unexpected): {}", ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }
}
