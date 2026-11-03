package com.finedge.service;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RepaymentCreateRequest;
import com.finedge.dto.RepaymentDTO;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanRepaymentServiceTest {

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
    private LoanRepaymentService repaymentService;

    private static final Integer APP_ID = 42;
    private static final String BORROWER_USER = "USR-BOR";

    private UserInfo borrowerUser() {
        UserInfo u = new UserInfo();
        u.setUserId(BORROWER_USER);
        return u;
    }

    private LoanApplication application(String status) {
        LoanApplication a = new LoanApplication();
        a.setId(APP_ID);
        a.setRef("LNA-REF");
        a.setStatus(status);
        return a;
    }

    private void asBorrower() {
        lenient().when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(BORROWER_USER));
        lenient().when(currentUserService.getCurrentUser()).thenReturn(Optional.of(borrowerUser()));
        lenient().when(currentUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_BORROWER"));
    }

    private RepaymentCreateRequest request(BigDecimal amount) {
        RepaymentCreateRequest r = new RepaymentCreateRequest();
        r.setRepaymentAmount(amount);
        return r;
    }

    @Test
    @DisplayName("Successful repayment of a disbursed loan")
    void recordRepayment_DisbursedLoan_Persists() {
        asBorrower();
        LoanApplication app = application("Approved");
        when(applicationRepository.findByIdAndBorrower_UserId(APP_ID, BORROWER_USER))
                .thenReturn(Optional.of(app));
        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setDisbursementId("DISB-1");
        disbursement.setApplicationId(APP_ID);
        disbursement.setDisbursedAmount(new BigDecimal("50000.00"));
        when(disbursementRepository.findByApplicationId(APP_ID)).thenReturn(Optional.of(disbursement));
        when(repaymentRepository.findByApplicationIdOrderByPaidAtDesc(APP_ID)).thenReturn(List.of());
        when(idGenerator.generateUnique(eq("REPAY"), any())).thenReturn("REPAY-A1B2C3");
        LoanRepayment saved = new LoanRepayment();
        saved.setRepaymentId("REPAY-A1B2C3");
        saved.setApplicationId(APP_ID);
        saved.setRepaymentAmount(new BigDecimal("5000.00"));
        saved.setPaidAt(LocalDateTime.now());
        saved.setPaidByUserId(BORROWER_USER);
        saved.setCreatedDatetime(LocalDateTime.now());
        when(repaymentRepository.save(any())).thenReturn(saved);

        RepaymentDTO dto = repaymentService.recordRepayment(APP_ID, request(new BigDecimal("5000.00")));

        assertNotNull(dto);
        assertEquals("REPAY-A1B2C3", dto.getRepaymentId());
        assertEquals(APP_ID, dto.getApplicationId());
        assertEquals(new BigDecimal("5000.00"), dto.getRepaymentAmount());
        assertNotNull(dto.getPaidAt());
        assertEquals(BORROWER_USER, dto.getPaidByUserId());

        ArgumentCaptor<LoanRepayment> captor = ArgumentCaptor.forClass(LoanRepayment.class);
        verify(repaymentRepository).save(captor.capture());
        LoanRepayment captured = captor.getValue();
        assertEquals(APP_ID, captured.getApplicationId());
        assertEquals(new BigDecimal("5000.00"), captured.getRepaymentAmount());
        assertEquals(BORROWER_USER, captured.getPaidByUserId());
        assertNotNull(captured.getPaidAt());
    }

    @Test
    @DisplayName("Repayment rejected before disbursement")
    void recordRepayment_BeforeDisbursement_Rejected() {
        asBorrower();
        when(applicationRepository.findByIdAndBorrower_UserId(APP_ID, BORROWER_USER))
                .thenReturn(Optional.of(application("Approved")));
        when(disbursementRepository.findByApplicationId(APP_ID)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> repaymentService.recordRepayment(APP_ID, request(new BigDecimal("5000.00"))));
        verify(repaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Repayment rejected for zero amount")
    void recordRepayment_ZeroAmount_Rejected() {
        asBorrower();
        when(applicationRepository.findByIdAndBorrower_UserId(APP_ID, BORROWER_USER))
                .thenReturn(Optional.of(application("Approved")));
        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setDisbursedAmount(new BigDecimal("50000.00"));
        lenient().when(disbursementRepository.findByApplicationId(APP_ID)).thenReturn(Optional.of(disbursement));
        lenient().when(repaymentRepository.findByApplicationIdOrderByPaidAtDesc(APP_ID)).thenReturn(List.of());

        assertThrows(BusinessRuleException.class,
                () -> repaymentService.recordRepayment(APP_ID, request(BigDecimal.ZERO)));
    }

    @Test
    @DisplayName("Repayment rejected for negative amount")
    void recordRepayment_NegativeAmount_Rejected() {
        asBorrower();
        when(applicationRepository.findByIdAndBorrower_UserId(APP_ID, BORROWER_USER))
                .thenReturn(Optional.of(application("Approved")));
        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setDisbursedAmount(new BigDecimal("50000.00"));
        lenient().when(disbursementRepository.findByApplicationId(APP_ID)).thenReturn(Optional.of(disbursement));
        lenient().when(repaymentRepository.findByApplicationIdOrderByPaidAtDesc(APP_ID)).thenReturn(List.of());

        assertThrows(BusinessRuleException.class,
                () -> repaymentService.recordRepayment(APP_ID, request(new BigDecimal("-100.00"))));
    }

    @Test
    @DisplayName("Repayment rejected when it exceeds the outstanding balance")
    void recordRepayment_ExcessiveAmount_Rejected() {
        asBorrower();
        when(applicationRepository.findByIdAndBorrower_UserId(APP_ID, BORROWER_USER))
                .thenReturn(Optional.of(application("Approved")));
        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setDisbursementId("DISB-1");
        disbursement.setApplicationId(APP_ID);
        disbursement.setDisbursedAmount(new BigDecimal("50000.00"));
        when(disbursementRepository.findByApplicationId(APP_ID)).thenReturn(Optional.of(disbursement));
        when(repaymentRepository.findByApplicationIdOrderByPaidAtDesc(APP_ID))
                .thenReturn(List.of(repayment(new BigDecimal("45000.00"))));

        // Outstanding is 5000.00; requesting 6000.00 exceeds it.
        assertThrows(BusinessRuleException.class,
                () -> repaymentService.recordRepayment(APP_ID, request(new BigDecimal("6000.00"))));
        verify(repaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Repayment exactly equal to outstanding balance is allowed")
    void recordRepayment_ExactOutstanding_Allowed() {
        asBorrower();
        when(applicationRepository.findByIdAndBorrower_UserId(APP_ID, BORROWER_USER))
                .thenReturn(Optional.of(application("Approved")));
        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setDisbursementId("DISB-1");
        disbursement.setApplicationId(APP_ID);
        disbursement.setDisbursedAmount(new BigDecimal("50000.00"));
        when(disbursementRepository.findByApplicationId(APP_ID)).thenReturn(Optional.of(disbursement));
        when(repaymentRepository.findByApplicationIdOrderByPaidAtDesc(APP_ID)).thenReturn(List.of());
        when(idGenerator.generateUnique(eq("REPAY"), any())).thenReturn("REPAY-1");
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RepaymentDTO dto = repaymentService.recordRepayment(APP_ID, request(new BigDecimal("50000.00")));

        assertEquals(new BigDecimal("50000.00"), dto.getRepaymentAmount());
    }

    @Test
    @DisplayName("Repayment requires borrower ownership of the application")
    void recordRepayment_NotOwned_NotFound() {
        asBorrower();
        when(applicationRepository.findByIdAndBorrower_UserId(APP_ID, BORROWER_USER))
                .thenReturn(Optional.empty());
        lenient().when(disbursementRepository.findByApplicationId(APP_ID))
                .thenReturn(Optional.of(disbursement(BigDecimal.TEN)));

        assertThrows(ResourceNotFoundException.class,
                () -> repaymentService.recordRepayment(APP_ID, request(new BigDecimal("10.00"))));
    }

    @Test
    @DisplayName("Repayment history lists persisted repayments for the application")
    void listRepayments_ReturnsPage() {
        asBorrower();
        when(applicationRepository.findByIdAndBorrower_UserId(APP_ID, BORROWER_USER))
                .thenReturn(Optional.of(application("Approved")));
        LoanRepayment r = new LoanRepayment();
        r.setRepaymentId("REPAY-1");
        r.setApplicationId(APP_ID);
        r.setRepaymentAmount(new BigDecimal("5000.00"));
        PageResponse<RepaymentDTO> page = new PageResponse<>(List.of(mapDto(r)), 0, 10, 1, 1, true, true);
        when(repaymentRepository.findByApplicationIdOrderByPaidAtDesc(eq(APP_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(r)));

        PageResponse<RepaymentDTO> result = repaymentService.listRepayments(APP_ID, 0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals("REPAY-1", result.getContent().get(0).getRepaymentId());
    }

    private LoanDisbursement disbursement(BigDecimal amount) {
        LoanDisbursement d = new LoanDisbursement();
        d.setDisbursedAmount(amount);
        return d;
    }

    private RepaymentDTO mapDto(LoanRepayment r) {
        RepaymentDTO dto = new RepaymentDTO();
        dto.setRepaymentId(r.getRepaymentId());
        dto.setRepaymentAmount(r.getRepaymentAmount());
        return dto;
    }

    private LoanRepayment repayment(BigDecimal amount) {
        LoanRepayment r = new LoanRepayment();
        r.setRepaymentId("REPAY-X");
        r.setApplicationId(APP_ID);
        r.setRepaymentAmount(amount);
        return r;
    }
}
