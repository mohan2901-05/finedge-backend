package com.finedge.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finedge.entity.UserInfo;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Unit tests for {@link JwtService}. No Spring context; the signing key is
 * derived from random bytes so no secret is hardcoded.
 */
class JwtServiceTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static String newSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void generateAndValidateRoundTrip() {
        String secret = newSecret();
        JwtService service = new JwtService(secret, 900_000L, "test-issuer");

        UserInfo user = new UserInfo();
        user.setUserId("USR-UNIT");
        user.setUserEmail("unit@example.invalid");

        String token = service.generateToken(user);
        assertNotNull(token);

        assertTrue(service.validateToken(token));
        assertEquals("USR-UNIT", service.extractUserId(token));
        assertEquals("unit@example.invalid",
                service.extractClaims(token).get("email", String.class));
        assertEquals("test-issuer", service.extractClaims(token).getIssuer());
    }

    @Test
    void expiredTokenIsRejected() {
        String secret = newSecret();
        JwtService service = new JwtService(secret, -1L, "test-issuer"); // immediately expired

        UserInfo user = new UserInfo();
        user.setUserId("USR-EXP");
        user.setUserEmail("exp@example.invalid");

        String token = service.generateToken(user);
        // An expired token must not validate.
        assertFalse(service.validateToken(token));
    }

    @Test
    void wrongSecretRejectsToken() {
        JwtService serviceA = new JwtService(newSecret(), 900_000L, "issuer");
        JwtService serviceB = new JwtService(newSecret(), 900_000L, "issuer");

        UserInfo user = new UserInfo();
        user.setUserId("USR-X");
        user.setUserEmail("x@example.invalid");

        String token = serviceA.generateToken(user);
        assertFalse(serviceB.validateToken(token));
    }

    @Test
    void constructionWithInvalidSecretFailsFast() {
        assertThrows(Exception.class,
                () -> new JwtService("not-valid-base64!!!", 900_000L, "issuer"));
    }

    @Test
    void malformedTokenRejected() {
        assertDoesNotThrow(() -> {
            JwtService service = new JwtService(newSecret(), 900_000L, "issuer");
            assertFalse(service.validateToken("not.a.valid.token"));
        });
    }
}
