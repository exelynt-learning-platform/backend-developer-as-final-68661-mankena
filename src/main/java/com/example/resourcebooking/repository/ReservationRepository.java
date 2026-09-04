package com.example.resourcebooking.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.resourcebooking.entity.Reservation;
import com.example.resourcebooking.entity.Resource;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long>,
                JpaSpecificationExecutor<Reservation> {

    /**
     * Finds any reservation for the given resource that overlaps the given
     * time window and is not CANCELLED. Used to block double-booking.
     * Overlap condition: existing.start < newEnd AND existing.end > newStart.
     * excludeId lets an update ignore the reservation being edited.
     */
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.resource = :resource " +
            "AND r.status <> com.example.resourcebooking.entity.ReservationStatus.CANCELLED " +
            "AND r.startTime < :endTime " +
            "AND r.endTime > :startTime " +
            "AND (:excludeId IS NULL OR r.id <> :excludeId)")
    List<Reservation> findOverlapping(
            @Param("resource") Resource resource,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId);
}
