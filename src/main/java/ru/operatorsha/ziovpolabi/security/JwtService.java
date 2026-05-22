package ru.operatorsha.ziovpolabi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.key = Keys.hmacShaKeyFor(normalizeSecret(secret).getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(UserDetails userDetails, UUID userId) {
        return buildToken(userDetails, userId, accessTokenExpirationMs, Map.of(
                "type", "access",
                "roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
        ));
    }

    public String generateRefreshToken(UserDetails userDetails, UUID userId, String sessionId) {
        return buildToken(userDetails, userId, refreshTokenExpirationMs, Map.of(
                "type", "refresh",
                "sessionId", sessionId
        ));
    }

    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    public String getUsername(String token) {
        return parse(token).getPayload().getSubject();
    }

    public boolean isValidAccessToken(String token) {
        return isValidTokenOfType(token, "access");
    }

    public boolean isValidRefreshToken(String token) {
        return isValidTokenOfType(token, "refresh");
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    private String buildToken(UserDetails userDetails, UUID userId, long ttlMs, Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .claim("userId", userId.toString())
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key)
                .compact();
    }

    private boolean isValidTokenOfType(String token, String type) {
        try {
            Jws<Claims> claims = parse(token);
            return type.equals(claims.getPayload().get("type", String.class))
                    && claims.getPayload().getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    private String normalizeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        if (secret.length() >= 32) {
            return secret;
        }
        return secret + "0".repeat(32 - secret.length());
    }
}
