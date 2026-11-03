package com.finedge.security;

import com.finedge.entity.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import javax.crypto.SecretKey;

/**
 * Issues and validates short-lived JWT access tokens.
 *
 * <p>The token subject is the user ID. Claims carry email, role and org for
 * downstream authorization groundwork without requiring a database lookup on
 * every request. Signing material is loaded from configuration
 * ({@code finedge.jwt.secret}) and is never hardcoded.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final String issuer;

    public JwtService(@Value("${finedge.jwt.secret}") String secret,
                      @Value("${finedge.jwt.expiration-ms:900000}") long expirationMs,
                      @Value("${finedge.jwt.issuer:finedge}") String issuer) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    /**
     * Generates an access token for the authenticated user.
     */
    public String generateToken(UserInfo user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(user.getUserId())
                .claim("email", user.getUserEmail())
                .claim("roleId", roleId(user))
                .claim("roleName", roleName(user))
                .claim("orgId", orgId(user))
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parses and validates the token signature, issuer and expiration.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            // Malformed, unsigned, expired, or otherwise invalid token.
            return false;
        }
    }

    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private String roleId(UserInfo user) {
        return user.getRole() != null ? user.getRole().getRoleId() : null;
    }

    private String roleName(UserInfo user) {
        return user.getRole() != null ? user.getRole().getRoleName() : null;
    }

    private String orgId(UserInfo user) {
        return user.getOrganization() != null ? user.getOrganization().getOrgId() : null;
    }
}
