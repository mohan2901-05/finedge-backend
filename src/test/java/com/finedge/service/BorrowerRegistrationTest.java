package com.finedge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finedge.dto.AuthRegisterRequest;
import com.finedge.dto.UserDTO;
import com.finedge.entity.UserInfo;
import com.finedge.repository.UserInfoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Tests borrower registration against the V7-seeded BORROWER role
 * (ROLE-T04, ORG-T001).
 *
 * <p>Uses {@code ORG-T001} / {@code ROLE-T04}, which are seeded by the V2
 * and V7 Flyway migrations. Verifies that registration assigns the V7
 * borrower role and organisation, never FINANCIER_ADMIN.
 */
@Transactional
@SpringBootTest(
        properties = {
                "finedge.auth.default-org-id=ORG-T001",
                "finedge.auth.default-borrower-role-id=ROLE-T04"
        }
)
class BorrowerRegistrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserInfoRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationAssignsV7BorrowerRoleAndOrg() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUserName("Borrower User");
        request.setPassword("Secret123!");
        request.setUserEmail("borrower-v7@example.invalid");

        UserDTO dto = authService.register(request);

        assertNotNull(dto.getUserId());
        assertEquals("ORG-T001", dto.getOrgId());
        assertEquals("ROLE-T04", dto.getRoleId());

        Optional<UserInfo> stored = userRepository.findByUserEmail("borrower-v7@example.invalid");
        assertTrue(stored.isPresent());
        assertEquals("ROLE-T04", stored.get().getRole().getRoleId());
        assertEquals("ORG-T001", stored.get().getOrganization().getOrgId());
        assertTrue(passwordEncoder.matches("Secret123!", stored.get().getPassword()));
    }

    @Test
    void registrationDoesNotAssignFinancierAdmin() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUserName("Borrower User 2");
        request.setPassword("Secret123!");
        request.setUserEmail("borrower-v7-2@example.invalid");

        UserDTO dto = authService.register(request);

        assertEquals("ROLE-T04", dto.getRoleId());
        assertEquals("ORG-T001", dto.getOrgId());
    }
}
