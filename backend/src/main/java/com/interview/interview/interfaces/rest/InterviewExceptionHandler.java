package com.interview.interview.interfaces.rest;

import com.interview.execution.ExamConfigNotFoundException;
import com.interview.interview.application.ContainerNotReadyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {InterviewController.class, CheckpointController.class, FileController.class})
class InterviewExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleIllegalState(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ExamConfigNotFoundException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ProblemDetail handleExamConfigNotFound(ExamConfigNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                "此面試需由面試官確認結果：" + ex.getMessage());
    }

    @ExceptionHandler(ContainerNotReadyException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)   // 503 — frontend should keep polling
    ProblemDetail handleContainerNotReady(ContainerNotReadyException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }
}
