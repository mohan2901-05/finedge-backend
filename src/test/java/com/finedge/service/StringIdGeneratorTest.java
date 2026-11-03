package com.finedge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link StringIdGenerator} (no Spring context needed).
 */
class StringIdGeneratorTest {

    private final StringIdGenerator generator = new StringIdGenerator();

    @Test
    @DisplayName("Generated IDs match PREFIX-XXXXXX format and VARCHAR(50)")
    void generate_FormatAndLength() {
        String id = generator.generate("ORG");
        assertTrue(id.matches("ORG-[A-Z2-9]{6}"), "Unexpected format: " + id);
        assertTrue(id.length() <= 50, "ID must fit VARCHAR(50)");
    }

    @Test
    @DisplayName("Prefixes are normalized to upper-case")
    void generate_PrefixNormalized() {
        assertTrue(generator.generate("usr").startsWith("USR-"));
    }

    @Test
    @DisplayName("Blank or illegal prefixes are rejected")
    void generate_BadPrefixRejected() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(null));
        assertThrows(IllegalArgumentException.class, () -> generator.generate("  "));
        assertThrows(IllegalArgumentException.class, () -> generator.generate("ORG!"));
        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(" prefix-that-is-way-too-long "));
    }

    @Test
    @DisplayName("10k IDs via generateUnique are all distinct and well-formed")
    void generate_UniqueBulk() {
        // NOTE: raw generate() draws collide with ~5% probability at 10k
        // (birthday paradox on a 32^6 space), so bulk uniqueness must go
        // through generateUnique -- which guarantees it by construction.
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            String id = generator.generateUnique("LOAN", seen::contains);
            assertTrue(id.matches("LOAN-[A-Z2-9]{6}"), "Unexpected format: " + id);
            assertTrue(seen.add(id), "Duplicate from generateUnique");
        }
        assertEquals(10_000, seen.size());
    }

    @Test
    @DisplayName("generateUnique retries past collisions and exhausts loudly")
    void generateUnique_RetryAndExhaustion() {
        Set<String> taken = new HashSet<>();
        String first = generator.generateUnique("MAP", taken::contains);
        taken.add(first);
        String second = generator.generateUnique("MAP", taken::contains);
        assertTrue(!first.equals(second));

        assertThrows(IllegalStateException.class,
                () -> generator.generateUnique("MAP", s -> true));
    }
}
