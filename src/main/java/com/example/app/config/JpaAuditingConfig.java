package com.example.app.config;

import com.example.app.util.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /** Fills created_by / updated_by with the authenticated user's email. */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return SecurityUtils::currentUsername;
    }
}
