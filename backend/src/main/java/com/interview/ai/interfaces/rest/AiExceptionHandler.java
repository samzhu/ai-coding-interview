package com.interview.ai.interfaces.rest;

import com.interview.ai.application.AiDisabledException;
import com.interview.interview.InterviewExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AiChatController.class)
class AiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        log.warn("AI chat bad request: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AiDisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ProblemDetail handleAiDisabled(AiDisabledException ex) {
        log.warn("AI disabled: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InterviewExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    ProblemDetail handleExpired(InterviewExpiredException ex) {
        log.warn("Interview expired for AI chat: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
    }
}
