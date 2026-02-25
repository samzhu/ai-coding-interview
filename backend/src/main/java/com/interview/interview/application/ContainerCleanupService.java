package com.interview.interview.application;

import com.interview.execution.ContainerService;
import com.interview.interview.domain.Interview;
import com.interview.interview.domain.InterviewStatus;
import com.interview.interview.infrastructure.persistence.InterviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContainerCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ContainerCleanupService.class);

    private final InterviewRepository interviewRepository;
    private final ContainerService containerService;

    public ContainerCleanupService(InterviewRepository interviewRepository,
                                   ContainerService containerService) {
        this.interviewRepository = interviewRepository;
        this.containerService = containerService;
    }

    /**
     * Periodically clean up orphaned containers from interviews that are no longer in progress.
     * This is a safety net for server crashes or unclean shutdowns.
     */
    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    @Transactional
    public void cleanupOrphanedContainers() {
        var interviews = interviewRepository.findAll();
        for (Interview interview : interviews) {
            String containerId = interview.getContainerId();
            if (containerId == null || containerId.isBlank()) continue;

            InterviewStatus status = interview.getInterviewStatus();
            if (status == InterviewStatus.COMPLETED || status == InterviewStatus.CANCELLED) {
                log.info("Cleaning up orphaned container {} for {} interview {}",
                        containerId, status, interview.getId());
                try {
                    containerService.stopContainer(containerId);
                    interview.clearContainer();
                    interviewRepository.save(interview);
                } catch (Exception e) {
                    log.warn("Failed to clean up container {}: {}", containerId, e.getMessage());
                }
            }
        }
    }
}
