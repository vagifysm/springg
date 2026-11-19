package com.example.app.config;

import com.example.app.constant.SecurityConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * Declares a Bearer-JWT scheme. Controllers that need it are annotated with
     * {@code @SecurityRequirement(name = SecurityConstants.BEARER_SCHEME)}, which gives the
     * Swagger UI an "Authorize" button and lock icons on the protected endpoints.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot Starter Template API")
                        .description("Reusable backend template: JWT auth, PostgreSQL, Liquibase, Docker.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(SecurityConstants.BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the accessToken returned by /auth/login (without the 'Bearer ' prefix).")));
    }
}
