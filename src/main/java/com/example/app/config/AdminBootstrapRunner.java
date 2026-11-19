package com.example.app.config;

import com.example.app.entity.User;
import com.example.app.enums.Role;
import com.example.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Creates the first ADMIN user from ADMIN_EMAIL / ADMIN_PASSWORD, if configured.
 * Public registration can only ever create USER accounts, so this is how you get an admin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.email())) {
            return;
        }
        if (!StringUtils.hasText(properties.password())) {
            throw new IllegalStateException("ADMIN_EMAIL is set but ADMIN_PASSWORD is empty");
        }
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            log.info("Bootstrap admin {} already exists - skipping", email);
            return;
        }
        User admin = new User();
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(properties.password()));
        admin.setFullName(properties.fullName());
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Created bootstrap admin {}", email);
    }
}
