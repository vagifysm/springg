package com.example.app.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        /* Base64-encoded HMAC key (>= 32 bytes). Supplied through the JWT_SECRET environment variable. */
        @NotBlank String secret,
        @DefaultValue("60m") Duration accessTokenTtl,
        @DefaultValue("spring-boot-starter-template") String issuer
) {
}
