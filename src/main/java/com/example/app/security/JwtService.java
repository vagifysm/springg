package com.example.app.security;

import com.example.app.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Creates and validates signed JWT access tokens (HMAC). */
@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final String issuer;

    public JwtService(JwtProperties properties) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(properties.secret());
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) must be a Base64 value - generate one with: openssl rand -base64 48", ex);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must decode to at least 32 bytes (256 bits)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtl = properties.accessTokenTtl();
        this.issuer = properties.issuer();
    }

    public String generateToken(CustomUserDetails principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(principal.getUsername())
                .claim(ROLE_CLAIM, principal.getUser().getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies signature, expiry and issuer, then returns the subject (the user's email).
     *
     * @throws io.jsonwebtoken.JwtException if the token is invalid, tampered with or expired
     */
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public long getExpiresInSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
