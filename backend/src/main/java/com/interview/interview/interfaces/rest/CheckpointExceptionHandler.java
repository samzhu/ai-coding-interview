package com.interview.interview.interfaces.rest;

import com.interview.interview.InterviewExpiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CheckpointController.class)
class CheckpointExceptionHandler {

    @ExceptionHandler(InterviewExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    ProblemDetail handleExpired(InterviewExpiredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleConflict(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
