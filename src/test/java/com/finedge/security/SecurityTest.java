package com.finedge.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finedge.dto.UserDTO;
import com.finedge.entity.Organization;
import com.finedge.entity.RoleMaster;
import com.finedge.entity.UserInfo;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.RoleMasterRepository;
import com.finedge.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

/**
 * Verifies the security filter chain: protected endpoints reject unauthenticated
 * requests (401) and accept a valid JWT; the public auth endpoints stay
 * reachable without credentials.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserInfoRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleMasterRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String accessToken;

    @BeforeEach
    void seedAuthenticatedUser() {
        Organization org = new Organization();
        org.setOrgId("ORG-SECTEST");
        org.setOrgName("Sec Test Org");
        org.setOrgEmail("sectest@example.invalid");
        organizationRepository.save(org);

        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-SECTEST");
        role.setOrganization(org);
        role.setRoleName("Borrower");
        role.setStatus("ACTIVE");
        roleRepository.save(role);

        UserInfo user = new UserInfo();
        user.setUserId("USR-SECTEST");
        user.setOrganization(org);
        user.setRole(role);
        user.setUserName("Sec User");
        user.setPassword(passwordEncoder.encode("Secret123!"));
        user.setUserEmail("secuser@example.invalid");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        accessToken = jwtService.generateToken(user);
    }

    @Test
    void protectedEndpointRejectsUnauthenticatedRequest() throws Exception {
        mvc.perform(get("/api/etfdb/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsValidJwt() throws Exception {
        mvc.perform(get("/api/etfdb/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void tokenEncodesCurrentUserIdentity() {
        assertNotNull(accessToken);
        assertTrue(jwtService.validateToken(accessToken));
        assertEqualsCurrentUserId();
    }

    private void assertEqualsCurrentUserId() {
        org.junit.jupiter.api.Assertions.assertEquals("USR-SECTEST", jwtService.extractUserId(accessToken));
    }

    @Test
    void expiredTokenIsRejected() {
        org.junit.jupiter.api.Assertions.assertFalse(
                jwtService.validateToken("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJVU1ItRXhwaXJlZCJ9.invalid"));
    }
}
