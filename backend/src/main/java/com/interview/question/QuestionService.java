package com.interview.question;

import com.interview.question.application.QuestionQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionQueryService queryService;

    public QuestionService(QuestionQueryService queryService) {
        this.queryService = queryService;
    }

    public List<QuestionDetail> findAllQuestions() {
        return queryService.findAll();
    }

    public QuestionDetail getQuestion(String id) {
        return queryService.findById(id);
    }
}
