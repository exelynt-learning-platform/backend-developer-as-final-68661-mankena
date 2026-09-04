package com.example.resourcebooking.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.repository.ResourceRepository;
import com.example.resourcebooking.exception.ResourceNotFoundException;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    public Resource createResource(Resource resource) {
        return resourceRepository.save(resource);
    }

    public Resource updateResource(Long id, Resource updatedResource) {

        Resource existingResource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));

        existingResource.setName(updatedResource.getName());
        existingResource.setDescription(updatedResource.getDescription());
        existingResource.setType(updatedResource.getType());
        existingResource.setPrice(updatedResource.getPrice());
        existingResource.setAvailable(updatedResource.isAvailable());

        return resourceRepository.save(existingResource);
    }

    public void deleteResource(Long id) {

        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));

        resourceRepository.delete(resource);
    }
}