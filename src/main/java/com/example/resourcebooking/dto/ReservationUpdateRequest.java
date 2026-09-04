package com.example.resourcebooking.dto;

import java.time.LocalDateTime;

import com.example.resourcebooking.entity.ReservationStatus;

import jakarta.validation.constraints.NotNull;

/**
 * Input DTO for updating a reservation.
 * `status` is only honored when the caller is an ADMIN - the service layer
 * enforces this, not this class.
 */
public class ReservationUpdateRequest {

    @NotNull(message = "resourceId is required")
    private Long resourceId;

    @NotNull(message = "startTime is required")
    private LocalDateTime startTime;

    @NotNull(message = "endTime is required")
    private LocalDateTime endTime;

    private ReservationStatus status;

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

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
