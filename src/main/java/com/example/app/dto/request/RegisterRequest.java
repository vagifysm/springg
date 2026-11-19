package com.example.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "jane@example.com")
        @NotBlank @Email @Size(max = 255)
        String email,

        // BCrypt only uses the first 72 bytes, so longer passwords are rejected.
        @Schema(example = "S3cure-passw0rd")
        @NotBlank @Size(min = 8, max = 72)
        String password,

        @Schema(example = "Jane Doe")
        @NotBlank @Size(max = 150)
        String fullName
) {
}
