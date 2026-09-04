package com.example.resourcebooking.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.resourcebooking.dto.ReservationRequest;
import com.example.resourcebooking.dto.ReservationUpdateRequest;
import com.example.resourcebooking.entity.AppUser;
import com.example.resourcebooking.entity.Reservation;
import com.example.resourcebooking.entity.ReservationStatus;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.exception.ConflictException;
import com.example.resourcebooking.exception.ForbiddenOperationException;
import com.example.resourcebooking.exception.ResourceNotFoundException;
import com.example.resourcebooking.repository.ReservationRepository;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserService userService;
    private final ResourceService resourceService;

    public ReservationService(
            ReservationRepository reservationRepository,
            UserService userService,
            ResourceService resourceService) {

        this.reservationRepository = reservationRepository;
        this.userService = userService;
        this.resourceService = resourceService;
    }

    /**
     * Builds one combined Specification from whichever filters were supplied,
     * so status + minPrice + maxPrice can all apply together correctly
     * instead of the old if/else chain that silently dropped filters.
     */
    public Page<Reservation> getReservations(
            String username,
            boolean isAdmin,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        List<Specification<Reservation>> specs = new ArrayList<>();

        if (!isAdmin) {
            AppUser user = userService.getUserByUsername(username);
            specs.add((root, query, cb) -> cb.equal(root.get("user"), user));
        }

        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (minPrice != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }

        if (maxPrice != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        Specification<Reservation> combined = specs.stream()
                .reduce(Specification::and)
                .orElse(null);

        return reservationRepository.findAll(combined, pageable);
    }

    public Reservation getReservationById(Long id, String username, boolean isAdmin) {

        Reservation reservation = findByIdOrThrow(id);

        assertOwnerOrAdmin(reservation, username, isAdmin);

        return reservation;
    }

    public Reservation createReservation(
            ReservationRequest request,
            String username) {

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        AppUser user = userService.getUserByUsername(username);

        Resource resource = resourceService.getResourceById(request.getResourceId());

        if (!resource.isAvailable()) {
            throw new ConflictException(
                    "Resource '" + resource.getName() + "' is not available for booking");
        }

        List<Reservation> overlapping = reservationRepository.findOverlapping(
                resource, request.getStartTime(), request.getEndTime(), null);

        if (!overlapping.isEmpty()) {
            throw new ConflictException(
                    "Resource is already booked for an overlapping time slot");
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setPrice(calculatePrice(resource, request.getStartTime(), request.getEndTime()));

        return reservationRepository.save(reservation);
    }

    public Reservation updateReservation(
            Long id,
            ReservationUpdateRequest request,
            String username,
            boolean isAdmin) {

        Reservation existing = findByIdOrThrow(id);

        assertOwnerOrAdmin(existing, username, isAdmin);

        if (!isAdmin && existing.getStatus() != ReservationStatus.PENDING) {
            throw new ForbiddenOperationException(
                    "Only pending reservations can be modified");
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        Resource resource = resourceService.getResourceById(request.getResourceId());

        if (!resource.isAvailable()) {
            throw new ConflictException(
                    "Resource '" + resource.getName() + "' is not available for booking");
        }

        List<Reservation> overlapping = reservationRepository.findOverlapping(
                resource, request.getStartTime(), request.getEndTime(), id);

        if (!overlapping.isEmpty()) {
            throw new ConflictException(
                    "Resource is already booked for an overlapping time slot");
        }

        existing.setResource(resource);
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setPrice(calculatePrice(resource, request.getStartTime(), request.getEndTime()));

        if (isAdmin && request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        return reservationRepository.save(existing);
    }

    public void deleteReservation(Long id, String username, boolean isAdmin) {

        Reservation reservation = findByIdOrThrow(id);

        assertOwnerOrAdmin(reservation, username, isAdmin);

        reservationRepository.delete(reservation);
    }

    private Reservation findByIdOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found with id: " + id));
    }

    private void assertOwnerOrAdmin(Reservation reservation, String username, boolean isAdmin) {

        if (isAdmin) {
            return;
        }

        if (!reservation.getUser().getUsername().equals(username)) {
            throw new ForbiddenOperationException(
                    "You do not have permission to access this reservation");
        }
    }

    /**
     * Price is always computed server-side from the resource's hourly rate
     * and the requested duration - never trusted from client input.
     */
    private BigDecimal calculatePrice(Resource resource, java.time.LocalDateTime start, java.time.LocalDateTime end) {

        double hours = Duration.between(start, end).toMinutes() / 60.0;

        return resource.getPrice()
                .multiply(BigDecimal.valueOf(hours))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
