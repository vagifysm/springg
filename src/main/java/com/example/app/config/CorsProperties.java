package com.example.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @DefaultValue("http://localhost:3000") List<String> allowedOrigins,
        @DefaultValue({"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}) List<String> allowedMethods,
        @DefaultValue("*") List<String> allowedHeaders,
        @DefaultValue("Location") List<String> exposedHeaders,
        @DefaultValue("false") boolean allowCredentials,
        @DefaultValue("1h") Duration maxAge
) {
}
