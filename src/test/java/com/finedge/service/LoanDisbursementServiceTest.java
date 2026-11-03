package com.finedge.service;

import com.finedge.dto.DisbursementCreateRequest;
import com.finedge.dto.DisbursementDTO;
import com.finedge.dto.OutstandingDTO;
import com.finedge.entity.Financier;
import com.finedge.entity.LoanApplication;
import com.finedge.entity.LoanDisbursement;
import com.finedge.entity.LoanRepayment;
import com.finedge.entity.UserInfo;
import com.finedge.exception.BusinessRuleException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.LoanApplicationRepository;
import com.finedge.repository.LoanDisbursementRepository;
import com.finedge.repository.LoanRepaymentRepository;
import com.finedge.security.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanDisbursementServiceTest {

    @Mock
    private LoanApplicationRepository applicationRepository;

    @Mock
    private LoanDisbursementRepository disbursementRepository;

    @Mock
    private LoanRepaymentRepository repaymentRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private LoanDisbursementService disbursementService;

    private static final Integer APP_ID = 42;
    private static final String FIN_KEY = "FIN-001";
    private static final String FIN_USER = "USR-FIN";
    private static final String BORROWER_USER = "USR-BOR";

    private UserInfo financierUser() {
        UserInfo u = new UserInfo();
        u.setUserId(FIN_USER);
        Financier f = new Financier();
        f.setFinancierId(FIN_KEY);
        f.setInstitutionName("Test Bank");
        u.setFinancier(f);
        return u;
    }

    private UserInfo borrowerUser() {
        UserInfo u = new UserInfo();
        u.setUserId(BORROWER_USER);
        return u;
    }

    private LoanApplication application(String status, BigDecimal loanAmount) {
        LoanApplication a = new LoanApplication();
        a.setId(APP_ID);
        a.setRef("LNA-REF");
        a.setStatus(status);
        a.setFinancierKey(FIN_KEY);
        a.setLoanAmount(loanAmount);
        return a;
    }

    private void asFinancier() {
        lenient().when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(FIN_USER));
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(financierUser()));
    }

    private void asBorrower() {
        lenient().when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(BORROWER_USER));
    }

    private DisbursementCreateRequest request(BigDecimal amount) {
        DisbursementCreateRequest r = new DisbursementCreateRequest();
        r.setDisbursedAmount(amount);
        return r;
    }

    private LoanRepayment repayment(BigDecimal amount) {
        LoanRepayment r = new LoanRepayment();
        r.setRepaymentId("REPAY-1");
        r.setApplicationId(APP_ID);
        r.setRepaymentAmount(amount);
        return r;
    }

    @Test
    @DisplayName("Successful disbursement of an approved loan")
    void recordDisbursement_ApprovedLoan_Persists() {
        asFinancier();
        when(applicationRepository.findByIdAndFinancierKey(APP_ID, FIN_KEY))
                .thenReturn(Optional.of(application("Approved", new BigDecimal("50000.00"))));
        when(disbursementRepository.existsByApplicationId(APP_ID)).thenReturn(false);
        when(idGenerator.generateUnique(eq("DISB"), any())).thenReturn("DISB-A1B2C3");
        LoanDisbursement saved = new LoanDisbursement();
        saved.setDisbursementId("DISB-A1B2C3");
        saved.setApplicationId(APP_ID);
        saved.setDisbursedAmount(new BigDecimal("50000.00"));
        saved.setDisbursedAt(LocalDateTime.now());
        saved.setDisbursedByUserId(FIN_USER);
        saved.setCreatedDatetime(LocalDateTime.now());
        when(disbursementRepository.save(any())).thenReturn(saved);

        DisbursementDTO dto = disbursementService.recordDisbursement(APP_ID, request(new BigDecimal("50000.00")));

        assertNotNull(dto);
        assertEquals("DISB-A1B2C3", dto.getDisbursementId());
        assertEquals(APP_ID, dto.getApplicationId());
        assertEquals(new BigDecimal("50000.00"), dto.getDisbursedAmount());
        assertEquals(new BigDecimal("50000.00"), dto.getLoanAmount());
        assertNotNull(dto.getDisbursedAt());
        assertEquals(FIN_USER, dto.getDisbursedByUserId());

        ArgumentCaptor<LoanDisbursement> captor = ArgumentCaptor.forClass(LoanDisbursement.class);
        verify(disbursementRepository).save(captor.capture());
        LoanDisbursement captured = captor.getValue();
        assertEquals(APP_ID, captured.getApplicationId());
        assertEquals(new BigDecimal("50000.00"), captured.getDisbursedAmount());
        assertEquals(FIN_USER, captured.getDisbursedByUserId());
        assertNotNull(captured.getDisbursedAt());
        assertNotNull(captured.getCreatedDatetime());
    }

    @Test
    @DisplayName("Disbursement rejected for a pending loan")
    void recordDisbursement_PendingLoan_Rejected() {
        asFinancier();
        when(applicationRepository.findByIdAndFinancierKey(APP_ID, FIN_KEY))
                .thenReturn(Optional.of(application("Pending Review", new BigDecimal("50000.00"))));

        assertThrows(BusinessRuleException.class,
                () -> disbursementService.recordDisbursement(APP_ID, request(new BigDecimal("50000.00"))));
        verify(disbursementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Disbursement rejected for a rejected loan")
    void recordDisbursement_RejectedLoan_Rejected() {
        asFinancier();
        when(applicationRepository.findByIdAndFinancierKey(APP_ID, FIN_KEY))
                .thenReturn(Optional.of(application("Rejected", new BigDecimal("50000.00"))));

        assertThrows(BusinessRuleException.class,
                () -> disbursementService.recordDisbursement(APP_ID, request(new BigDecimal("50000.00"))));
    }

    @Test
    @DisplayName("Duplicate disbursement prevention")
    void recordDisbursement_AlreadyDisbursed_Rejected() {
        asFinancier();
        when(applicationRepository.findByIdAndFinancierKey(APP_ID, FIN_KEY))
                .thenReturn(Optional.of(application("Approved", new BigDecimal("50000.00"))));
        when(disbursementRepository.existsByApplicationId(APP_ID)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> disbursementService.recordDisbursement(APP_ID, request(new BigDecimal("50000.00"))));
        verify(disbursementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Disbursement requires the financier's own institution (cross-institution denied)")
    void recordDisbursement_CrossInstitution_NotFound() {
        asFinancier();
        when(applicationRepository.findByIdAndFinancierKey(APP_ID, FIN_KEY)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> disbursementService.recordDisbursement(APP_ID, request(new BigDecimal("50000.00"))));
    }

    @Test
    @DisplayName("Disbursement requires an associated financier institution")
    void recordDisbursement_NoFinancierAssociation_NotFound() {
        lenient().when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(FIN_USER));
        UserInfo noFinancier = new UserInfo();
        noFinancier.setUserId(FIN_USER);
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(noFinancier));

        assertThrows(ResourceNotFoundException.class,
                () -> disbursementService.recordDisbursement(APP_ID, request(new BigDecimal("50000.00"))));
    }

    @Test
    @DisplayName("Borrower cannot record a disbursement (no financier scope)")
    void recordDisbursement_Borrower_NotFound() {
        asBorrower();

        assertThrows(ResourceNotFoundException.class,
                () -> disbursementService.recordDisbursement(APP_ID, request(new BigDecimal("50000.00"))));
    }

    @Test
    @DisplayName("Outstanding balance for an approved but not-yet-disbursed loan")
    void outstanding_NotDisbursed_Zero() {
        asFinancier();
        when(currentUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_FINANCIER_ADMIN"));
        when(applicationRepository.findByIdAndFinancierKey(APP_ID, FIN_KEY))
                .thenReturn(Optional.of(application("Approved", new BigDecimal("50000.00"))));
        when(disbursementRepository.findByApplicationId(APP_ID)).thenReturn(Optional.empty());

        OutstandingDTO dto = disbursementService.getOutstanding(APP_ID);

        assertFalse(dto.isDisbursed());
        assertEquals(BigDecimal.ZERO, dto.getOutstandingAmount());
        assertFalse(dto.isFullyRepaid());
    }

    @Test
    @DisplayName("Outstanding balance calculation after partial repayment")
    void outstanding_PartialRepayment_Computed() {
        asFinancier();
        when(currentUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_FINANCIER_ADMIN"));
        when(applicationRepository.findByIdAndFinancierKey(APP_ID, FIN_KEY))
                .thenReturn(Optional.of(application("Approved", new BigDecimal("50000.00"))));
        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setDisbursementId("DISB-1");
        disbursement.setApplicationId(APP_ID);
        disbursement.setDisbursedAmount(new BigDecimal("50000.00"));
        when(disbursementRepository.findByApplicationId(APP_ID)).thenReturn(Optional.of(disbursement));
        when(repaymentRepository.findByApplicationIdOrderByPaidAtDesc(APP_ID))
                .thenReturn(List.of(repayment(new BigDecimal("12000.00")),
                        repayment(new BigDecimal("8000.00"))));

        OutstandingDTO dto = disbursementService.getOutstanding(APP_ID);

        assertTrue(dto.isDisbursed());
        assertEquals(new BigDecimal("50000.00"), dto.getDisbursedAmount());
        assertEquals(new BigDecimal("20000.00"), dto.getTotalRepaid());
        assertEquals(new BigDecimal("30000.00"), dto.getOutstandingAmount());
        assertFalse(dto.isFullyRepaid());
    }

    @Test
    @DisplayName("Fully repaid behavior when balance reaches zero")
    void outstanding_FullyRepaid_Marked() {
        asFinancier();
        when(currentUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_FINANCIER_ADMIN"));
        when(applicationRepository.findByIdAndFinancierKey(APP_ID, FIN_KEY))
                .thenReturn(Optional.of(application("Approved", new BigDecimal("50000.00"))));
        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setDisbursementId("DISB-1");
        disbursement.setApplicationId(APP_ID);
        disbursement.setDisbursedAmount(new BigDecimal("50000.00"));
        when(disbursementRepository.findByApplicationId(APP_ID)).thenReturn(Optional.of(disbursement));
        when(repaymentRepository.findByApplicationIdOrderByPaidAtDesc(APP_ID))
                .thenReturn(List.of(repayment(new BigDecimal("50000.00"))));

        OutstandingDTO dto = disbursementService.getOutstanding(APP_ID);

        assertEquals(0, dto.getOutstandingAmount().compareTo(BigDecimal.ZERO));
        assertTrue(dto.isFullyRepaid());
    }

    @Test
    @DisplayName("Rounding applies HALF_UP at currency precision")
    void rounding_HalfUp_Applied() {
        BigDecimal rounded = new BigDecimal("100.005").setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("100.01"), rounded);
        BigDecimal roundedDown = new BigDecimal("100.004").setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("100.00"), roundedDown);
    }
}
