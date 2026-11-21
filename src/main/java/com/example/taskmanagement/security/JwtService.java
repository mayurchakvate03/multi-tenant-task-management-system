package com.example.taskmanagement.security;

import com.example.taskmanagement.domain.AppUser;
import com.example.taskmanagement.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    public String createToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("tenant_id", user.getTenantId())
                .claim("user_id", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    public TenantUserPrincipal parsePrincipal(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tenantId = claims.get("tenant_id", String.class);
        String email = claims.getSubject();
        String roleName = claims.get("role", String.class);
        Number userId = claims.get("user_id", Number.class);
        if (tenantId == null || tenantId.isBlank() || email == null || email.isBlank()
                || roleName == null || userId == null) {
            throw new IllegalArgumentException("JWT is missing required claims");
        }
        return new TenantUserPrincipal(userId.longValue(), email, tenantId, UserRole.valueOf(roleName));
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
