package com.example.app.dto.response;

import com.example.app.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        Instant createdAt
) {
}
