package com.interview.interview;

import com.interview.interview.infrastructure.persistence.InterviewRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InterviewModelProvider {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private final InterviewRepository repository;

    public InterviewModelProvider(InterviewRepository repository) {
        this.repository = repository;
    }

    public String getAiModel(UUID interviewId) {
        return repository.findById(interviewId)
                .map(interview -> {
                    String model = interview.getAiModel();
                    return (model != null && !model.isBlank()) ? model : DEFAULT_MODEL;
                })
                .orElse(DEFAULT_MODEL);
    }
}
