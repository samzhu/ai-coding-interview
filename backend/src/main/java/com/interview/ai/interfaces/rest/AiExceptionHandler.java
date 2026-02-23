package com.interview.ai.interfaces.rest;

import com.interview.ai.application.AiDisabledException;
import com.interview.interview.InterviewExpiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AiChatController.class)
class AiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AiDisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ProblemDetail handleAiDisabled(AiDisabledException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InterviewExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    ProblemDetail handleExpired(InterviewExpiredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
    }
}
