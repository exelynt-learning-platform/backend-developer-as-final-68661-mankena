package com.example.resourcebooking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.service.ResourceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResponseEntity<List<Resource>> getAllResources() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getResourceById(
            @PathVariable Long id) {

        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @PostMapping
    public ResponseEntity<Resource> createResource(
            @Valid @RequestBody Resource resource) {

        Resource createdResource = resourceService.createResource(resource);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdResource);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Resource> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody Resource resource) {

        Resource updatedResource =
                resourceService.updateResource(id, resource);

        return ResponseEntity.ok(updatedResource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(
            @PathVariable Long id) {

        resourceService.deleteResource(id);

        return ResponseEntity.noContent().build();
    }
}