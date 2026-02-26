package com.interview.interview.interfaces.rest;

/**
 * Response body for GET /api/v1/interviews/{id}/container-status.
 *
 * @param status         "INITIALIZING" | "READY" | "FAILED" | null (no container configured)
 * @param containerReady true when a container ID has been assigned and is ready for use
 */
public record ContainerStatusResponse(String status, boolean containerReady) {
}
