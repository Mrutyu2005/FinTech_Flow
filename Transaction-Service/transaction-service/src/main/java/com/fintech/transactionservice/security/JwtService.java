package com.fintech.transactionservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JwtService for Transaction-Service.
 * Does NOT generate tokens — only validates tokens issued by Account-Service.
 * Extracts username and role from the embedded claims.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extracts the role claim from the JWT.
     * Returns "USER" as default if the claim is absent.
     */
    public String extractRole(String token) {
        Claims claims = getClaims(token);
        Object role = claims.get("role");
        return role != null ? role.toString() : "USER";
    }

    public boolean isValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
