package com.example.resourcebooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.resourcebooking.entity.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

}