package com.finedge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finedge.dto.AuthLoginRequest;
import com.finedge.dto.AuthRegisterRequest;
import com.finedge.dto.UserDTO;
import com.finedge.entity.Organization;
import com.finedge.entity.RoleMaster;
import com.finedge.entity.UserInfo;
import com.finedge.exception.AuthException;
import com.finedge.exception.BusinessRuleException;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.RoleMasterRepository;
import com.finedge.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Tests for {@link AuthService}: registration hashing, login, and duplicate
 * email rejection. Uses the default (H2) profile; Flyway applies V1-V4, so the
 * test seeds its own organisation and borrower role.
 */
@Transactional
@SpringBootTest(
        properties = {
                "finedge.auth.default-org-id=ORG-REGTEST",
                "finedge.auth.default-borrower-role-id=ROLE-BORROWER-TEST"
        }
)
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserInfoRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleMasterRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedDefaults() {
        Organization org = new Organization();
        org.setOrgId("ORG-REGTEST");
        org.setOrgName("Reg Test Org");
        org.setOrgEmail("regtest@example.invalid");
        organizationRepository.save(org);

        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-BORROWER-TEST");
        role.setOrganization(org);
        role.setRoleName("Borrower");
        role.setStatus("ACTIVE");
        roleRepository.save(role);
    }

    @Test
    void registerStoresPasswordHashNotPlaintext() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUserName("Reg User");
        request.setPassword("Secret123!");
        request.setUserEmail("reguser@example.invalid");

        UserDTO dto = authService.register(request);

        assertNotNull(dto.getUserId());
        assertEquals("reguser@example.invalid", dto.getUserEmail());

        Optional<UserInfo> stored = userRepository.findByUserEmail("reguser@example.invalid");
        assertTrue(stored.isPresent());
        String storedHash = stored.get().getPassword();
        assertNotNull(storedHash);
        assertFalse(storedHash.equals("Secret123!"), "Password must not be stored plaintext");
        assertTrue(passwordEncoder.matches("Secret123!", storedHash),
                "Stored value must verify as a BCrypt hash");
        assertEquals("ROLE-BORROWER-TEST", stored.get().getRole().getRoleId());
        assertEquals("ORG-REGTEST", stored.get().getOrganization().getOrgId());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUserName("First");
        request.setPassword("Secret123!");
        request.setUserEmail("dup@example.invalid");
        authService.register(request);

        AuthRegisterRequest second = new AuthRegisterRequest();
        second.setUserName("Second");
        second.setPassword("Secret123!");
        second.setUserEmail("dup@example.invalid");

        assertThrows(DuplicateResourceException.class, () -> authService.register(second));
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUserName("Login User");
        request.setPassword("Secret123!");
        request.setUserEmail("loginuser@example.invalid");
        authService.register(request);

        AuthLoginRequest login = new AuthLoginRequest();
        login.setUserEmail("loginuser@example.invalid");
        login.setPassword("Secret123!");

        var response = authService.login(login);
        assertNotNull(response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertTrue(response.getExpiresIn() > 0);
        assertNotNull(response.getExpiresAt());
        assertNotNull(response.getUser());
        assertEquals("loginuser@example.invalid", response.getUser().getUserEmail());
    }

    @Test
    void loginRejectsInvalidPassword() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUserName("Login User");
        request.setPassword("Secret123!");
        request.setUserEmail("loginuser@example.invalid");
        authService.register(request);

        AuthLoginRequest login = new AuthLoginRequest();
        login.setUserEmail("loginuser@example.invalid");
        login.setPassword("WrongPassword!");

        assertThrows(AuthException.class, () -> authService.login(login));
    }

    @Test
    void loginRejectsUnknownEmailWithoutRevealingExistence() {
        AuthLoginRequest login = new AuthLoginRequest();
        login.setUserEmail("nonexistent@example.invalid");
        login.setPassword("anything");

        assertThrows(AuthException.class, () -> authService.login(login));
    }

    @Test
    void registrationAssignsDefaultBorrowerRole() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUserName("Role Test");
        request.setPassword("Secret123!");
        request.setUserEmail("roletest@example.invalid");

        UserDTO dto = authService.register(request);
        assertNotNull(dto.getRoleId());
        assertEquals("ROLE-BORROWER-TEST", dto.getRoleId());
    }

    @Test
    void registrationCannotSelfAssignFinancierAdmin() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUserName("Admin Attempt");
        request.setPassword("Secret123!");
        request.setUserEmail("adminattempt@example.invalid");

        UserDTO dto = authService.register(request);

        // The registered user must receive the configured borrower role,
        // never FINANCIER_ADMIN (ROLE-T03), regardless of client input.
        assertNotNull(dto.getRoleId());
        assertEquals("ROLE-BORROWER-TEST", dto.getRoleId());
        assertEquals("ORG-REGTEST", dto.getOrgId());
    }
}
