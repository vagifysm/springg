package com.example.app.constant;

public final class SecurityConstants {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE = "Bearer";

    /** Name of the OpenAPI security scheme (see OpenApiConfig). */
    public static final String BEARER_SCHEME = "bearerAuth";

    /** Endpoints reachable without a JWT. Everything else requires authentication. */
    public static final String[] PUBLIC_ENDPOINTS = {
            ApiPaths.AUTH + "/register",
            ApiPaths.AUTH + "/login",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health/**",
            "/actuator/info",
            "/error"
    };

    private SecurityConstants() {
    }
}
