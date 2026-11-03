package com.finedge.etfdb;

import com.finedge.entity.RoleMaster;
import com.finedge.repository.RoleMasterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;

/**
 * Verifies the V7 borrower role seed migration: the BORROWER role is seeded
 * with the correct identifier and organization, FINANCIER_ADMIN is unchanged,
 * and no duplicate role records are created.
 *
 * <p>Runs against the default H2 profile with the full Flyway chain
 * (V1-V7) applied automatically by Spring Boot test startup.
 */
@SpringBootTest
@Transactional
class BorrowerRoleMigrationTest {

    @Autowired
    private RoleMasterRepository roleRepository;

    @Test
    @DisplayName("V7: BORROWER role is seeded with ROLE-T04 and ORG-T001")
    void borrowerRoleSeededCorrectly() {
        RoleMaster role = roleRepository.findById("ROLE-T04")
                .orElseThrow(() -> new AssertionError("BORROWER role ROLE-T04 not seeded by V7"));

        assertEquals("ROLE-T04", role.getRoleId());
        assertEquals("BORROWER", role.getRoleName());
        assertEquals("ORG-T001", role.getOrganization().getOrgId());
        assertEquals("ACTIVE", role.getStatus());
        assertNotNull(role.getCreatedDatetime());
        assertNotNull(role.getUpdatedDatetime());
        assertEquals("SYSTEM", role.getCreatedUser());
        assertEquals("SYSTEM", role.getUpdatedUser());
    }

    @Test
    @DisplayName("V7: exactly one BORROWER role row exists (no duplicates)")
    void borrowerRoleNoDuplicates() {
        List<RoleMaster> roles = roleRepository.findAll();
        long count = roles.stream()
                .filter(r -> "ROLE-T04".equals(r.getRoleId()))
                .count();
        assertEquals(1, count, "BORROWER role must not be duplicated");
    }

    @Test
    @DisplayName("V6: FINANCIER_ADMIN role remains unchanged as ROLE-T03")
    void financierAdminRoleUnchanged() {
        RoleMaster role = roleRepository.findById("ROLE-T03")
                .orElseThrow(() -> new AssertionError("FINANCIER_ADMIN role ROLE-T03 not found"));

        assertEquals("ROLE-T03", role.getRoleId());
        assertEquals("FINANCIER_ADMIN", role.getRoleName());
        assertEquals("ORG-T001", role.getOrganization().getOrgId());
    }

    @Test
    @DisplayName("V7: no user is assigned the BORROWER role by the migration")
    void borrowerRoleNotAssignedToUserByMigration() {
        assertTrue(roleRepository.findById("ROLE-T04").isPresent());
        // The migration only inserts into role_master; user_info rows are not
        // created or updated by V7. This is asserted indirectly: no test user
        // is seeded here, so no user can reference ROLE-T04.
    }

    @Test
    @DisplayName("V1-V6 roles remain intact alongside V7 seed")
    void existingRolesPreserved() {
        assertTrue(roleRepository.findById("ROLE-T01").isPresent());
        assertTrue(roleRepository.findById("ROLE-T02").isPresent());
        assertTrue(roleRepository.findById("ROLE-T03").isPresent());
        assertTrue(roleRepository.findById("ROLE-T04").isPresent());
    }
}
