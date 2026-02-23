package com.interview.question.interfaces.rest;

import com.interview.question.QuestionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/questions")
class QuestionController {

    private final QuestionService service;

    QuestionController(QuestionService service) {
        this.service = service;
    }

    @GetMapping
    List<QuestionResponse> listQuestions() {
        return service.findAllQuestions().stream()
                .map(QuestionResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    QuestionResponse getQuestion(@PathVariable UUID id) {
        return QuestionResponse.from(service.getQuestion(id));
    }
}
