package com.example.app.security;

import com.example.app.config.JwtProperties;
import com.example.app.entity.User;
import com.example.app.enums.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static String randomSecret(int bytes) {
        byte[] key = new byte[bytes];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    private static CustomUserDetails principal() {
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("irrelevant");
        user.setFullName("Jane");
        user.setRole(Role.USER);
        return new CustomUserDetails(user);
    }

    private static JwtService service(String secret, Duration ttl) {
        return new JwtService(new JwtProperties(secret, ttl, "test-issuer"));
    }

    @Test
    void generatedToken_roundTripsToTheSubject() {
        JwtService jwtService = service(randomSecret(64), Duration.ofMinutes(5));

        String token = jwtService.generateToken(principal());

        assertThat(jwtService.extractUsername(token)).isEqualTo("jane@example.com");
        assertThat(jwtService.getExpiresInSeconds()).isEqualTo(300);
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        String token = service(randomSecret(64), Duration.ofMinutes(5)).generateToken(principal());
        JwtService otherService = service(randomSecret(64), Duration.ofMinutes(5));

        assertThatThrownBy(() -> otherService.extractUsername(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedToken_isRejected() {
        JwtService jwtService = service(randomSecret(64), Duration.ofMinutes(5));
        String token = jwtService.generateToken(principal());
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThatThrownBy(() -> jwtService.extractUsername(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredToken_isRejected() {
        JwtService jwtService = service(randomSecret(64), Duration.ofSeconds(-5));
        String token = jwtService.generateToken(principal());

        assertThatThrownBy(() -> jwtService.extractUsername(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tooShortSecret_failsFast() {
        String shortSecret = randomSecret(16);

        assertThatThrownBy(() -> service(shortSecret, Duration.ofMinutes(5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void nonBase64Secret_failsFast() {
        assertThatThrownBy(() -> service("not base64 !!!", Duration.ofMinutes(5)))
                .isInstanceOf(IllegalStateException.class);
    }
}
