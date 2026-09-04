package com.example.resourcebooking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.resourcebooking.entity.AppUser;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}
