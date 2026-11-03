package com.finedge.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.finedge.dto.AuthRegisterRequest;
import com.finedge.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies fail-closed behavior when borrower registration configuration is
 * missing or blank. Registration must not succeed without a valid default
 * organisation and borrower role.
 */
@Transactional
@SpringBootTest(
        properties = {
                "finedge.auth.default-org-id=",
                "finedge.auth.default-borrower-role-id="
        }
)
class BorrowerRegistrationConfigTest {

    @Autowired
    private AuthService authService;

    @Test
    void missingDefaultOrgFailsClosed() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUserName("No Org");
        request.setPassword("Secret123!");
        request.setUserEmail("no-org@example.invalid");

        assertThrows(BusinessRuleException.class, () -> authService.register(request));
    }

    @Test
    void missingDefaultBorrowerRoleFailsClosed() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUserName("No Role");
        request.setPassword("Secret123!");
        request.setUserEmail("no-role@example.invalid");

        assertThrows(BusinessRuleException.class, () -> authService.register(request));
    }
}
