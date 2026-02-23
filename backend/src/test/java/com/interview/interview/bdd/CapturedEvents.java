package com.interview.interview.bdd;

import com.interview.interview.InterviewCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
class CapturedEvents {

    private final List<InterviewCompletedEvent> completedEvents = new ArrayList<>();

    @EventListener
    public void onInterviewCompleted(InterviewCompletedEvent event) {
        completedEvents.add(event);
    }

    public boolean hasCompletedEventFor(UUID interviewId) {
        return completedEvents.stream()
                .anyMatch(e -> e.interviewId().equals(interviewId));
    }

    public void clear() {
        completedEvents.clear();
    }
}
