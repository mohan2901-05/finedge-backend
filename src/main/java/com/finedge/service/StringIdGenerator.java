package com.finedge.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.function.Predicate;

/**
 * Application-assigned string-ID generator for the 11 {@code VARCHAR(50)}
 * primary keys in {@code etfdb_dev.sql} (approved format: {@code ORG-A3K9P2}).
 *
 * <p>Q2 finding: the dump establishes only examples ({@code ORG001},
 * {@code USR001}, {@code ROLE001}, 8-digit numerics) with no documented
 * generator, trigger, or sequence. This component implements the approved
 * {@code PREFIX-XXXXXX} format instead of inventing per-table sequences.
 * {@code loan_applications.id} is excluded (auto-increment {@code INT}).
 *
 * <p>Format: {@code PREFIX} (caller-supplied, e.g. {@code ORG}, {@code USR},
 * {@code LOAN}) + {@code '-'} + 6 chars from Crockford base32 without
 * confusables ({@code I, L, O, U} and {@code 0, 1} omitted), drawn from
 * {@link SecureRandom} (32^6 ≈ 1B combinations per prefix). Total length is
 * bounded well within {@code VARCHAR(50)}.
 *
 * <p>Collision safety: the generator is pure randomness; uniqueness is
 * enforced by the database PK/UQ plus {@link #generateUnique}, which retries
 * against an existence check and fails fast instead of looping forever.
 */
@Component
public class StringIdGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static final int RANDOM_PART_LENGTH = 6;

    private static final int MAX_ATTEMPTS = 100;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates one ID, e.g. {@code ORG-A3K9P2}.
     *
     * @param prefix table prefix such as {@code ORG}; must be 1-20 chars of
     *               {@code [A-Z0-9]} (normalized to upper-case)
     * @return the generated ID, never null
     * @throws IllegalArgumentException if the prefix is missing or illegal
     */
    public String generate(String prefix) {
        String clean = normalizePrefix(prefix);
        StringBuilder sb = new StringBuilder(clean.length() + 1 + RANDOM_PART_LENGTH);
        sb.append(clean).append('-');
        for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Generates an ID that is not already taken, retrying on collision.
     *
     * @param prefix table prefix such as {@code USR}
     * @param exists returns {@code true} when a candidate ID already exists
     *               (typically {@code repository::existsById})
     * @return a candidate for which {@code exists} returned {@code false}
     * @throws IllegalStateException if no free ID is found within
     *                               {@value #MAX_ATTEMPTS} attempts
     */
    public String generateUnique(String prefix, Predicate<String> exists) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String candidate = generate(prefix);
            if (!exists.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique ID with prefix '" + prefix
                        + "' after " + MAX_ATTEMPTS + " attempts");
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("ID prefix must not be blank");
        }
        String clean = prefix.trim().toUpperCase();
        if (clean.length() > 20 || !clean.matches("[A-Z0-9]+")) {
            throw new IllegalArgumentException(
                    "ID prefix must be 1-20 chars of [A-Z0-9]: '" + prefix + "'");
        }
        return clean;
    }
}
