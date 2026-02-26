package com.interview.interview.interfaces.rest;

import com.interview.interview.application.CreateInterviewCommand;
import com.interview.interview.application.InterviewService;
import com.interview.interview.domain.Interview;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews")
class InterviewController {

    private final InterviewService service;

    InterviewController(InterviewService service) {
        this.service = service;
    }

    @GetMapping
    List<InterviewResponse> listInterviews() {
        return service.findAll().stream()
                .map(InterviewResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    InterviewResponse getInterview(@PathVariable UUID id) {
        Interview interview = service.findById(id);
        return InterviewResponse.from(interview);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    InterviewResponse createInterview(@RequestBody @Valid CreateInterviewRequest request) {
        CreateInterviewCommand command = new CreateInterviewCommand(
                request.candidateId(),
                request.interviewerId(),
                request.title(),
                request.scheduledAt(),
                request.questionId(),
                request.durationMinutes());
        Interview interview = service.createInterview(command);
        return InterviewResponse.from(interview);
    }

    @PostMapping("/{id}/start")
    InterviewResponse startInterview(@PathVariable UUID id) {
        Interview interview = service.startInterview(id);
        return InterviewResponse.from(interview);
    }

    @PostMapping("/{id}/complete")
    InterviewResponse completeInterview(@PathVariable UUID id) {
        Interview interview = service.completeInterview(id);
        return InterviewResponse.from(interview);
    }

    @PostMapping("/{id}/cancel")
    InterviewResponse cancelInterview(@PathVariable UUID id) {
        Interview interview = service.cancelInterview(id);
        return InterviewResponse.from(interview);
    }

    @GetMapping("/{id}/time-remaining")
    TimeRemainingResponse getTimeRemaining(@PathVariable UUID id) {
        return service.getTimeRemaining(id);
    }
}
