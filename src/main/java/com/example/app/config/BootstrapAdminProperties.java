package com.example.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Optional first ADMIN account, created on startup when an email is configured. */
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(
        String email,
        String password,
        @DefaultValue("Administrator") String fullName
) {
}
