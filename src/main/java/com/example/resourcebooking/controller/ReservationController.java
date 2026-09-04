package com.example.resourcebooking.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.resourcebooking.dto.ReservationRequest;
import com.example.resourcebooking.dto.ReservationUpdateRequest;
import com.example.resourcebooking.entity.Reservation;
import com.example.resourcebooking.entity.ReservationStatus;
import com.example.resourcebooking.service.ReservationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(
            ReservationService reservationService) {

        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<Page<Reservation>> getReservations(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(required = false)
            ReservationStatus status,

            @RequestParam(required = false)
            BigDecimal minPrice,

            @RequestParam(required = false)
            BigDecimal maxPrice,

            @RequestParam(defaultValue = "id")
            String sortBy,

            @RequestParam(defaultValue = "asc")
            String direction,

            Authentication authentication) {

        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(sortDirection, sortBy));

        Page<Reservation> reservations =
                reservationService.getReservations(
                        authentication.getName(),
                        isAdmin(authentication),
                        status,
                        minPrice,
                        maxPrice,
                        pageable);

        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationById(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                reservationService.getReservationById(
                        id,
                        authentication.getName(),
                        isAdmin(authentication)));
    }

    @PostMapping
    public ResponseEntity<Reservation> createReservation(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {

        Reservation createdReservation =
                reservationService.createReservation(
                        request,
                        authentication.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdReservation);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reservation> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                reservationService.updateReservation(
                        id,
                        request,
                        authentication.getName(),
                        isAdmin(authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(
            @PathVariable Long id,
            Authentication authentication) {

        reservationService.deleteReservation(
                id,
                authentication.getName(),
                isAdmin(authentication));

        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
