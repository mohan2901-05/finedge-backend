package com.finedge.service;

import com.finedge.dto.LoanApplicationCreateRequest;
import com.finedge.dto.LoanApplicationDTO;
import com.finedge.dto.LoanApplicationUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.Financier;
import com.finedge.entity.LoanApplication;
import com.finedge.entity.UserInfo;
import com.finedge.exception.BusinessRuleException;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.FinancierRepository;
import com.finedge.repository.LoanApplicationRepository;
import com.finedge.security.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository applicationRepository;

    @Mock
    private FinancierRepository financierRepository;

    @Mock
    private EmiCalculationService emiCalculationService;

    @Mock
    private StringIdGenerator idGenerator;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private LoanApplicationService applicationService;

    private LoanApplicationCreateRequest createRequest() {
        LoanApplicationCreateRequest request = new LoanApplicationCreateRequest();
        request.setCustomerName("Test Borrower");
        request.setCustomerPhone("9000000001");
        request.setCustomerEmail("a@example.invalid");
        request.setCustomerCity("Test City");
        request.setCustomerAddress("1 Test Road");
        request.setFinancierKey("test_fin");
        request.setFinancierLabel("Test Finance");
        request.setLoanAmount(new BigDecimal("50000.00"));
        request.setTenureMonths(12);
        request.setInterestRate(new BigDecimal("12.00"));
        return request;
    }

    private UserInfo borrower(String userId) {
        UserInfo u = new UserInfo();
        u.setUserId(userId);
        return u;
    }

    @Test
    @DisplayName("Create persists the intake with server-calculated financial figures and borrower")
    void create_Persists() {
        when(currentUserService.getCurrentUserId()).thenReturn(java.util.Optional.of("USR-TEST"));
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(borrower("USR-TEST")));
        when(idGenerator.generateUnique(eq("LNA"), any()))
                .thenReturn("LNA-A1B2C3");
        when(financierRepository.findById("test_fin")).thenReturn(Optional.of(financier("test_fin", "ACTIVE")));
        when(emiCalculationService.calculate(
                new BigDecimal("50000.00"), new BigDecimal("12.00"), 12))
                .thenReturn(new EmiCalculationService.EmiCalculationResult(
                        new BigDecimal("4442.63"), new BigDecimal("3311.56"), new BigDecimal("53311.56")));
        when(applicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanApplicationDTO created = applicationService.create(createRequest());

        assertNotNull(created.getRef());
        assertEquals("LNA-A1B2C3", created.getRef());
        assertEquals("Pending Review", created.getStatus());
        assertEquals("USR-TEST", created.getCustUserId());
        assertEquals(new BigDecimal("4442.63"), created.getEmi());
        assertEquals(new BigDecimal("3311.56"), created.getTotalInterest());
        assertEquals(new BigDecimal("53311.56"), created.getTotalPayable());
        verify(applicationRepository).save(any(LoanApplication.class));
    }

    @Test
    @DisplayName("Create rejects nonexistent financier")
    void create_NonexistentFinancierRejected() {
        when(currentUserService.getCurrentUserId()).thenReturn(java.util.Optional.of("USR-TEST"));
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(borrower("USR-TEST")));
        when(financierRepository.findById("test_fin")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> applicationService.create(createRequest()));
    }

    @Test
    @DisplayName("Create rejects inactive financier")
    void create_InactiveFinancierRejected() {
        when(currentUserService.getCurrentUserId()).thenReturn(java.util.Optional.of("USR-TEST"));
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(borrower("USR-TEST")));
        when(financierRepository.findById("test_fin")).thenReturn(Optional.of(financier("test_fin", "INACTIVE")));

        assertThrows(BusinessRuleException.class,
                () -> applicationService.create(createRequest()));
    }

    @Test
    @DisplayName("Create rejects unresolved borrower")
    void create_UnresolvedBorrowerRejected() {
        when(currentUserService.getCurrentUserId()).thenReturn(java.util.Optional.of("USR-TEST"));
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.create(createRequest()));
    }

    @Test
    @DisplayName("Create rejects unauthenticated user")
    void create_UnauthenticatedRejected() {
        when(currentUserService.getCurrentUserId()).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.create(createRequest()));
    }

    @Test
    @DisplayName("Update cannot change borrower ownership")
    void update_CannotChangeBorrower() {
        when(currentUserService.getCurrentUserId()).thenReturn(java.util.Optional.of("USR-TEST"));
        LoanApplication application = new LoanApplication();
        application.setId(7);
        application.setRef("LNA-OLD");
        application.setCustomerName("Old");
        application.setStatus("Pending Review");
        application.setBorrower(borrower("USR-TEST"));
        when(applicationRepository.findByIdAndBorrower_UserId(7, "USR-TEST"))
                .thenReturn(Optional.of(application));
        when(applicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanApplicationUpdateRequest request = new LoanApplicationUpdateRequest();
        request.setCustomerName(" New Name ");

        LoanApplicationDTO updated = applicationService.update(7, request);

        // Borrower is preserved
        assertEquals("USR-TEST", updated.getCustUserId());
        assertEquals("New Name", updated.getCustomerName());
    }

    @Test
    @DisplayName("Update recalculates financial figures when loan amount changes")
    void update_RecalculatesFinancials() {
        when(currentUserService.getCurrentUserId()).thenReturn(java.util.Optional.of("USR-TEST"));
        LoanApplication application = new LoanApplication();
        application.setId(7);
        application.setRef("LNA-OLD");
        application.setCustomerName("Old");
        application.setLoanAmount(new BigDecimal("50000.00"));
        application.setTenureMonths(12);
        application.setInterestRate(new BigDecimal("12.00"));
        application.setStatus("Pending Review");
        application.setBorrower(borrower("USR-TEST"));
        when(applicationRepository.findByIdAndBorrower_UserId(7, "USR-TEST"))
                .thenReturn(Optional.of(application));
        when(applicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(emiCalculationService.calculate(
                new BigDecimal("75000.00"), new BigDecimal("12.00"), 12))
                .thenReturn(new EmiCalculationService.EmiCalculationResult(
                        new BigDecimal("6663.95"), new BigDecimal("4967.40"), new BigDecimal("79967.40")));

        LoanApplicationUpdateRequest request = new LoanApplicationUpdateRequest();
        request.setLoanAmount(new BigDecimal("75000.00"));

        LoanApplicationDTO updated = applicationService.update(7, request);

        assertEquals(new BigDecimal("6663.95"), updated.getEmi());
        assertEquals(new BigDecimal("4967.40"), updated.getTotalInterest());
        assertEquals(new BigDecimal("79967.40"), updated.getTotalPayable());
    }

    @Test
    @DisplayName("Update cannot change financier to inactive one")
    void update_CannotSetInactiveFinancier() {
        when(currentUserService.getCurrentUserId()).thenReturn(java.util.Optional.of("USR-TEST"));
        LoanApplication application = new LoanApplication();
        application.setId(7);
        application.setRef("LNA-OLD");
        application.setBorrower(borrower("USR-TEST"));
        when(applicationRepository.findByIdAndBorrower_UserId(7, "USR-TEST"))
                .thenReturn(Optional.of(application));
        when(financierRepository.findById("inactive_fin")).thenReturn(Optional.of(financier("inactive_fin", "INACTIVE")));

        LoanApplicationUpdateRequest request = new LoanApplicationUpdateRequest();
        request.setFinancierKey("inactive_fin");

        assertThrows(BusinessRuleException.class,
                () -> applicationService.update(7, request));
    }

    private Financier financier(String id, String status) {
        Financier f = new Financier();
        f.setFinancierId(id);
        f.setInstitutionName("Test Finance");
        f.setStatus(status);
        return f;
    }

    private UserInfo financierUser(String userId, String financierId) {
        UserInfo u = new UserInfo();
        u.setUserId(userId);
        Financier f = new Financier();
        f.setFinancierId(financierId);
        f.setInstitutionName("Test Finance");
        f.setStatus("ACTIVE");
        u.setFinancier(f);
        return u;
    }

    private LoanApplication application(Integer id, String ref, String status, String financierKey) {
        LoanApplication a = new LoanApplication();
        a.setId(id);
        a.setRef(ref);
        a.setStatus(status);
        a.setFinancierKey(financierKey);
        a.setCustomerName("Test Customer");
        return a;
    }

    @Test
    @DisplayName("listForFinancier returns the current financier's applications")
    void listForFinancier_ReturnsFinancierApplications() {
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(financierUser("USR-FIN", "FIN-001")));
        when(applicationRepository.findByFinancierKey(eq("FIN-001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(application(1, "LNA-1", "Pending Review", "FIN-001"))));

        PageResponse<LoanApplicationDTO> page = applicationService.listForFinancier(null, 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("LNA-1", page.getContent().get(0).getRef());
    }

    @Test
    @DisplayName("listForFinancier filters by status")
    void listForFinancier_StatusFilter() {
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(financierUser("USR-FIN", "FIN-001")));
        when(applicationRepository.findByFinancierKeyAndStatus(eq("FIN-001"), eq("Approved"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(application(1, "LNA-1", "Approved", "FIN-001"))));

        PageResponse<LoanApplicationDTO> page = applicationService.listForFinancier("Approved", 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("Approved", page.getContent().get(0).getStatus());
    }

    @Test
    @DisplayName("listForFinancier rejects user with no financier association")
    void listForFinancier_NoFinancierAssociation() {
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(financierUser("USR-NO-FIN", null)));

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.listForFinancier(null, 0, 10));
    }

    @Test
    @DisplayName("listForFinancier rejects unauthenticated user")
    void listForFinancier_Unauthenticated() {
        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.listForFinancier(null, 0, 10));
    }

    @Test
    @DisplayName("getByIdForFinancier returns the application for the current financier")
    void getByIdForFinancier_ReturnsApplication() {
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(financierUser("USR-FIN", "FIN-001")));
        when(applicationRepository.findByIdAndFinancierKey(1, "FIN-001"))
                .thenReturn(java.util.Optional.of(application(1, "LNA-1", "Pending Review", "FIN-001")));

        LoanApplicationDTO dto = applicationService.getByIdForFinancier(1);

        assertEquals("LNA-1", dto.getRef());
        assertEquals("Pending Review", dto.getStatus());
    }

    @Test
    @DisplayName("getByIdForFinancier rejects when not owned by the financier")
    void getByIdForFinancier_NotOwned() {
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(financierUser("USR-FIN", "FIN-001")));
        when(applicationRepository.findByIdAndFinancierKey(1, "FIN-001"))
                .thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.getByIdForFinancier(1));
    }

    @Test
    @DisplayName("approve transitions Pending Review to Approved")
    void approve_TransitionsToApproved() {
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(financierUser("USR-FIN", "FIN-001")));
        LoanApplication app = application(1, "LNA-1", "Pending Review", "FIN-001");
        when(applicationRepository.findByIdAndFinancierKey(1, "FIN-001"))
                .thenReturn(java.util.Optional.of(app));
        when(applicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanApplicationDTO dto = applicationService.approve(1);

        assertEquals("Approved", dto.getStatus());
        assertEquals("Approved", app.getStatus());
    }

    @Test
    @DisplayName("approve rejects non-Pending Review status")
    void approve_RejectsNonPendingReview() {
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(financierUser("USR-FIN", "FIN-001")));
        LoanApplication app = application(1, "LNA-1", "Approved", "FIN-001");
        when(applicationRepository.findByIdAndFinancierKey(1, "FIN-001"))
                .thenReturn(java.util.Optional.of(app));

        assertThrows(BusinessRuleException.class,
                () -> applicationService.approve(1));
    }

    @Test
    @DisplayName("reject transitions Pending Review to Rejected")
    void reject_TransitionsToRejected() {
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(financierUser("USR-FIN", "FIN-001")));
        LoanApplication app = application(1, "LNA-1", "Pending Review", "FIN-001");
        when(applicationRepository.findByIdAndFinancierKey(1, "FIN-001"))
                .thenReturn(java.util.Optional.of(app));
        when(applicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanApplicationDTO dto = applicationService.reject(1);

        assertEquals("Rejected", dto.getStatus());
        assertEquals("Rejected", app.getStatus());
    }

    @Test
    @DisplayName("reject rejects non-Pending Review status")
    void reject_RejectsNonPendingReview() {
        when(currentUserService.getCurrentUser()).thenReturn(java.util.Optional.of(financierUser("USR-FIN", "FIN-001")));
        LoanApplication app = application(1, "LNA-1", "Rejected", "FIN-001");
        when(applicationRepository.findByIdAndFinancierKey(1, "FIN-001"))
                .thenReturn(java.util.Optional.of(app));

        assertThrows(BusinessRuleException.class,
                () -> applicationService.reject(1));
    }
}
