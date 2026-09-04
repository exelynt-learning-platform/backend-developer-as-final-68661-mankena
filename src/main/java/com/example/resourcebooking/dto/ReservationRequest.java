package com.example.resourcebooking.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

/**
 * Input DTO for creating/updating a reservation.
 * Deliberately excludes price, status, and user - those are always
 * derived/assigned server-side so a client can never set them directly.
 */
public class ReservationRequest {

    @NotNull(message = "resourceId is required")
    private Long resourceId;

    @NotNull(message = "startTime is required")
    @Future(message = "startTime must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "endTime is required")
    @Future(message = "endTime must be in the future")
    private LocalDateTime endTime;

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
