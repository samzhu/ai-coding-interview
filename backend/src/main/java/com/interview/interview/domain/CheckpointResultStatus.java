package com.interview.interview.domain;

public enum CheckpointResultStatus {

    PENDING {
        @Override
        public boolean canTransitionTo(CheckpointResultStatus target) {
            return target == IN_PROGRESS;
        }
    },
    IN_PROGRESS {
        @Override
        public boolean canTransitionTo(CheckpointResultStatus target) {
            return target == PASSED || target == FAILED;
        }
    },
    PASSED {
        @Override
        public boolean canTransitionTo(CheckpointResultStatus target) {
            return false;
        }
    },
    FAILED {
        @Override
        public boolean canTransitionTo(CheckpointResultStatus target) {
            return target == IN_PROGRESS;
        }
    };

    public abstract boolean canTransitionTo(CheckpointResultStatus target);
}
