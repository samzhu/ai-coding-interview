package com.interview.interview.application;

/**
 * Thrown when a file or checkpoint operation is requested while the interview's
 * container is still being initialised asynchronously (containerStatus = "INITIALIZING").
 *
 * Mapped to HTTP 503 Service Unavailable so the frontend can distinguish this
 * transient state from a permanent error and continue polling /container-status.
 */
public class ContainerNotReadyException extends RuntimeException {

    public ContainerNotReadyException(String message) {
        super(message);
    }
}
