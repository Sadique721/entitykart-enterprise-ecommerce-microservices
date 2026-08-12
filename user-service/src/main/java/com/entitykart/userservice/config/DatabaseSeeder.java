package com.entitykart.userservice.config;

import com.entitykart.userservice.entity.UserEntity;
import com.entitykart.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "mdsadiqueamin721721@gmail.com";
        java.util.Optional<UserEntity> existing = userRepository.findByEmail(adminEmail);
        if (existing.isEmpty()) {
            log.info("Seeding admin user: {}", adminEmail);
            UserEntity user = new UserEntity();
            user.setName("Md Sadique Amin");
            user.setEmail(adminEmail);
            user.setPassword(passwordEncoder.encode("Amin@123"));
            user.setRole("ADMIN");
            user.setActive(true);
            user.setGender("Male");
            user.setContactNum("+91 9999999999");
            user.setProfilePicURL("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Admin user successfully seeded.");
        } else {
            log.info("Admin user already exists. Updating password and role to ensure access.");
            UserEntity user = existing.get();
            user.setPassword(passwordEncoder.encode("Amin@123"));
            user.setRole("ADMIN");
            user.setActive(true);
            userRepository.save(user);
            log.info("Admin user credentials updated successfully.");
        }
    }
}
