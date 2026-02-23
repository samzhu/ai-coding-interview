package com.interview.interview.infrastructure.persistence;

import com.interview.interview.domain.Interview;
import org.springframework.data.repository.ListCrudRepository;

import java.util.UUID;

public interface InterviewRepository extends ListCrudRepository<Interview, UUID> {
}
