package com.example.resourcebooking.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.resourcebooking.entity.AppUser;
import com.example.resourcebooking.entity.Role;
import com.example.resourcebooking.repository.UserRepository;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (userRepository.findByUsername("admin").isEmpty()) {

                AppUser admin = new AppUser();

                admin.setUsername("admin");
                admin.setPassword(
                        passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);

                userRepository.save(admin);
            }

            if (userRepository.findByUsername("user").isEmpty()) {

                AppUser user = new AppUser();

                user.setUsername("user");
                user.setPassword(
                        passwordEncoder.encode("user123"));
                user.setRole(Role.USER);

                userRepository.save(user);
            }
        };
    }
}
