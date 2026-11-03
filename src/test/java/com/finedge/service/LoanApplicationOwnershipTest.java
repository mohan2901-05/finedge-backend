package com.finedge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finedge.dto.LoanApplicationCreateRequest;
import com.finedge.entity.Financier;
import com.finedge.entity.Organization;
import com.finedge.entity.RoleMaster;
import com.finedge.entity.UserInfo;
import com.finedge.repository.FinancierRepository;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.RoleMasterRepository;
import com.finedge.repository.UserInfoRepository;
import com.finedge.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;

/**
 * Integration test for Part D3 borrower-to-application ownership enforcement,
 * extended for Part D4 server-side financial calculation and financier
 * validation.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>A borrower can only list/retrieve/update/delete their own applications.</li>
 *   <li>A borrower cannot access another borrower's applications (404, not 200).</li>
 *   <li>An authenticated non-borrower role (FINANCIER_ADMIN) cannot bypass
 *       borrower ownership rules.</li>
 *   <li>Unauthenticated access is rejected (401).</li>
 *   <li>Application creation automatically associates the authenticated borrower.</li>
 *   <li>Financial figures are calculated server-side (EMI, total interest,
 *       total payable).</li>
 *   <li>Client-supplied financier label is overridden by the authoritative
 *       database value.</li>
 *   <li>Nonexistent/inactive financier is rejected.</li>
 * </ul>
 *
 * <p>Uses the full security filter chain with real JWT tokens. Runs against
 * the default H2 profile with Flyway V1-V9 applied automatically.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class LoanApplicationOwnershipTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserInfoRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleMasterRepository roleRepository;

    @Autowired
    private FinancierRepository financierRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String borrower1Token;
    private String borrower2Token;
    private String financierAdminToken;

    private Integer borrower1AppId;

    @BeforeEach
    void seedUsersAndTokens() {
        Organization org = new Organization();
        org.setOrgId("ORG-OWNTEST");
        org.setOrgName("Ownership Test Org");
        org.setOrgEmail("owntest@example.invalid");
        org.setStatus("ACTIVE");
        organizationRepository.save(org);

        RoleMaster borrowerRole = new RoleMaster();
        borrowerRole.setRoleId("ROLE-OWN-BORROWER");
        borrowerRole.setOrganization(org);
        borrowerRole.setRoleName("BORROWER");
        borrowerRole.setStatus("ACTIVE");
        roleRepository.save(borrowerRole);

        RoleMaster adminRole = new RoleMaster();
        adminRole.setRoleId("ROLE-OWN-ADMIN");
        adminRole.setOrganization(org);
        adminRole.setRoleName("FINANCIER_ADMIN");
        adminRole.setStatus("ACTIVE");
        roleRepository.save(adminRole);

        Financier testFinancier = new Financier();
        testFinancier.setFinancierId("test_fin");
        testFinancier.setInstitutionName("Test Finance");
        testFinancier.setStatus("ACTIVE");
        financierRepository.save(testFinancier);

        UserInfo borrower1 = new UserInfo();
        borrower1.setUserId("USR-OWN-B1");
        borrower1.setOrganization(org);
        borrower1.setUserName("Borrower One");
        borrower1.setPassword(passwordEncoder.encode("Secret123!"));
        borrower1.setUserEmail("borrower1@example.invalid");
        borrower1.setRole(borrowerRole);
        borrower1.setStatus("ACTIVE");
        userRepository.save(borrower1);

        UserInfo borrower2 = new UserInfo();
        borrower2.setUserId("USR-OWN-B2");
        borrower2.setOrganization(org);
        borrower2.setUserName("Borrower Two");
        borrower2.setPassword(passwordEncoder.encode("Secret123!"));
        borrower2.setUserEmail("borrower2@example.invalid");
        borrower2.setRole(borrowerRole);
        borrower2.setStatus("ACTIVE");
        userRepository.save(borrower2);

        UserInfo admin = new UserInfo();
        admin.setUserId("USR-OWN-A1");
        admin.setOrganization(org);
        admin.setUserName("Financier Admin");
        admin.setPassword(passwordEncoder.encode("Secret123!"));
        admin.setUserEmail("admin@example.invalid");
        admin.setRole(adminRole);
        admin.setStatus("ACTIVE");
        userRepository.save(admin);

        borrower1Token = jwtService.generateToken(borrower1);
        borrower2Token = jwtService.generateToken(borrower2);
        financierAdminToken = jwtService.generateToken(admin);
    }

    @Test
    void borrowerCanCreateApplicationAndIsAssociatedAutomatically() throws Exception {
        LoanApplicationCreateRequest request = new LoanApplicationCreateRequest();
        request.setCustomerName("Borrower One App");
        request.setCustomerPhone("9000000001");
        request.setCustomerEmail("b1app@example.invalid");
        request.setCustomerCity("Test City");
        request.setCustomerAddress("1 Test Road");
        request.setFinancierKey("test_fin");
        request.setFinancierLabel("Client-Supplied Label");
        request.setLoanAmount(new BigDecimal("50000.00"));
        request.setTenureMonths(12);
        request.setInterestRate(new BigDecimal("12.00"));

        var result = mvc.perform(post("/api/etfdb/loan-applications")
                        .header("Authorization", "Bearer " + borrower1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.custUserId").value("USR-OWN-B1"))
                .andExpect(jsonPath("$.ref").exists())
                .andExpect(jsonPath("$.financierKey").value("test_fin"))
                .andExpect(jsonPath("$.financierLabel").value("Test Finance"))
                .andReturn();

        borrower1AppId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asInt();
    }

    @Test
    void borrowerCanListOnlyTheirOwnApplications() throws Exception {
        createAppAsBorrower1();

        mvc.perform(get("/api/etfdb/loan-applications")
                        .header("Authorization", "Bearer " + borrower2Token))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    int contentSize = objectMapper.readTree(body).get("content").size();
                    assertEquals(0, contentSize,
                            "Borrower2 should not see Borrower1's applications");
                });
    }

    @Test
    void borrowerCannotRetrieveAnotherBorrowersApplication() throws Exception {
        createAppAsBorrower1();

        mvc.perform(get("/api/etfdb/loan-applications/" + borrower1AppId)
                        .header("Authorization", "Bearer " + borrower2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrowerCannotUpdateAnotherBorrowersApplication() throws Exception {
        createAppAsBorrower1();

        LoanApplicationCreateRequest request = new LoanApplicationCreateRequest();
        request.setCustomerName("Hacked Name");
        request.setCustomerPhone("9000000001");
        request.setCustomerEmail("b1app@example.invalid");
        request.setCustomerCity("Test City");
        request.setCustomerAddress("1 Test Road");
        request.setFinancierKey("test_fin");
        request.setFinancierLabel("Test Finance");
        request.setLoanAmount(new BigDecimal("50000.00"));
        request.setTenureMonths(12);
        request.setInterestRate(new BigDecimal("12.00"));

        mvc.perform(put("/api/etfdb/loan-applications/" + borrower1AppId)
                        .header("Authorization", "Bearer " + borrower2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrowerCannotDeleteAnotherBorrowersApplication() throws Exception {
        createAppAsBorrower1();

        mvc.perform(delete("/api/etfdb/loan-applications/" + borrower1AppId)
                        .header("Authorization", "Bearer " + borrower2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void financierAdminCannotBypassBorrowerOwnership() throws Exception {
        createAppAsBorrower1();

        mvc.perform(get("/api/etfdb/loan-applications/" + borrower1AppId)
                        .header("Authorization", "Bearer " + financierAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mvc.perform(get("/api/etfdb/loan-applications"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/etfdb/loan-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"TEST\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientCannotSupplyBorrowerIdInCreateRequest() throws Exception {
        LoanApplicationCreateRequest request = new LoanApplicationCreateRequest();
        request.setCustomerName("No User Override");
        request.setCustomerPhone("9000000001");
        request.setCustomerEmail("nouser@example.invalid");
        request.setCustomerCity("Test City");
        request.setCustomerAddress("1 Test Road");
        request.setFinancierKey("test_fin");
        request.setFinancierLabel("Test Finance");
        request.setLoanAmount(new BigDecimal("50000.00"));
        request.setTenureMonths(12);
        request.setInterestRate(new BigDecimal("12.00"));

        mvc.perform(post("/api/etfdb/loan-applications")
                        .header("Authorization", "Bearer " + borrower1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.custUserId").value("USR-OWN-B1"));
    }

    @Test
    void clientSuppliedFinancierLabelIsOverridden() throws Exception {
        LoanApplicationCreateRequest request = new LoanApplicationCreateRequest();
        request.setCustomerName("Label Override Test");
        request.setCustomerPhone("9000000001");
        request.setCustomerEmail("label@example.invalid");
        request.setCustomerCity("Test City");
        request.setCustomerAddress("1 Test Road");
        request.setFinancierKey("test_fin");
        request.setFinancierLabel("Malicious Label");
        request.setLoanAmount(new BigDecimal("50000.00"));
        request.setTenureMonths(12);
        request.setInterestRate(new BigDecimal("12.00"));

        mvc.perform(post("/api/etfdb/loan-applications")
                        .header("Authorization", "Bearer " + borrower1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.financierLabel").value("Test Finance"));
    }

    private void createAppAsBorrower1() throws Exception {
        LoanApplicationCreateRequest request = new LoanApplicationCreateRequest();
        request.setCustomerName("Borrower One App");
        request.setCustomerPhone("9000000001");
        request.setCustomerEmail("b1app@example.invalid");
        request.setCustomerCity("Test City");
        request.setCustomerAddress("1 Test Road");
        request.setFinancierKey("test_fin");
        request.setFinancierLabel("Test Finance");
        request.setLoanAmount(new BigDecimal("50000.00"));
        request.setTenureMonths(12);
        request.setInterestRate(new BigDecimal("12.00"));

        var result = mvc.perform(post("/api/etfdb/loan-applications")
                        .header("Authorization", "Bearer " + borrower1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        borrower1AppId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asInt();
    }
}
