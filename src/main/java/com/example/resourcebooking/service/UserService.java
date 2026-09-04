package com.example.resourcebooking.service;

import org.springframework.stereotype.Service;

import com.example.resourcebooking.entity.AppUser;
import com.example.resourcebooking.repository.UserRepository;
import com.example.resourcebooking.exception.ResourceNotFoundException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser getUserByUsername(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username));
    }

    public AppUser getUserById(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));
    }

    public AppUser createUser(AppUser user) {
        return userRepository.save(user);
    }
}
