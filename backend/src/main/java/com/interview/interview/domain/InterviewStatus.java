package com.interview.interview.domain;

public enum InterviewStatus {

    SCHEDULED {
        @Override
        public boolean canTransitionTo(InterviewStatus target) {
            return target == IN_PROGRESS || target == CANCELLED;
        }
    },
    IN_PROGRESS {
        @Override
        public boolean canTransitionTo(InterviewStatus target) {
            return target == COMPLETED || target == CANCELLED;
        }
    },
    COMPLETED {
        @Override
        public boolean canTransitionTo(InterviewStatus target) {
            return false;
        }
    },
    CANCELLED {
        @Override
        public boolean canTransitionTo(InterviewStatus target) {
            return false;
        }
    };

    public abstract boolean canTransitionTo(InterviewStatus target);
}
