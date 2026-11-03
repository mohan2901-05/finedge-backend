package com.finedge.service;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RepaymentDetailCreateRequest;
import com.finedge.dto.RepaymentDetailDTO;
import com.finedge.entity.LoanInfo;
import com.finedge.entity.RepaymentDetail;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.LoanInfoRepository;
import com.finedge.repository.RepaymentDetailRepository;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    @Mock
    private RepaymentDetailRepository repaymentRepository;

    @Mock
    private LoanInfoRepository loanRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @InjectMocks
    private RepaymentService repaymentService;

    private RepaymentDetailCreateRequest createRequest() {
        RepaymentDetailCreateRequest request = new RepaymentDetailCreateRequest();
        request.setLoanId("LOAN-1");
        request.setInstallmentNumber(1);
        request.setDueDate(LocalDate.of(2026, 2, 1));
        request.setOpeningPrincipal(new BigDecimal("50000.00"));
        request.setInstallmentAmount(new BigDecimal("4500.00"));
        request.setPrincipal(new BigDecimal("4000.00"));
        request.setInterest(new BigDecimal("500.00"));
        request.setClosingPrincipal(new BigDecimal("46000.00"));
        request.setRate(new BigDecimal("12.00"));
        return request;
    }

    private RepaymentDetail existingRepayment() {
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        RepaymentDetail repayment = new RepaymentDetail();
        repayment.setRepaymentId("REP-1");
        repayment.setLoan(loan);
        repayment.setInstallmentNumber(1);
        repayment.setAdvanceFlag("N");
        return repayment;
    }

    @Test
    @DisplayName("Create assigns a generated ID with N default for advance flag")
    void create_AssignsIdWithDefaultFlag() {
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.of(loan));
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("REP-ABC123");
        when(repaymentRepository.save(any(RepaymentDetail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RepaymentDetailDTO created = repaymentService.create(createRequest());

        assertEquals("REP-ABC123", created.getRepaymentId());
        assertEquals("N", created.getAdvanceFlag());
        assertEquals("LOAN-1", created.getLoanId());
        ArgumentCaptor<RepaymentDetail> saved = ArgumentCaptor.forClass(RepaymentDetail.class);
        verify(repaymentRepository).save(saved.capture());
        assertEquals("REP-ABC123", saved.getValue().getRepaymentId());
        assertEquals("LOAN-1", saved.getValue().getLoan().getLoanId());
        assertEquals(Integer.valueOf(1), saved.getValue().getInstallmentNumber());
        assertEquals(LocalDate.of(2026, 2, 1), saved.getValue().getDueDate());
        assertEquals(new BigDecimal("50000.00"), saved.getValue().getOpeningPrincipal());
        assertEquals(new BigDecimal("4500.00"), saved.getValue().getInstallmentAmount());
        assertEquals(new BigDecimal("4000.00"), saved.getValue().getPrincipal());
        assertEquals(new BigDecimal("500.00"), saved.getValue().getInterest());
        assertEquals(new BigDecimal("46000.00"), saved.getValue().getClosingPrincipal());
        assertEquals(new BigDecimal("12.00"), saved.getValue().getRate());
        assertEquals("N", saved.getValue().getAdvanceFlag());
    }

    @Test
    @DisplayName("Create keeps an explicit advance flag")
    void create_KeepsExplicitFlag() {
        RepaymentDetailCreateRequest request = createRequest();
        request.setAdvanceFlag("Y");
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.of(loan));
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("REP-ABC123");
        when(repaymentRepository.save(any(RepaymentDetail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RepaymentDetailDTO created = repaymentService.create(request);

        assertEquals("Y", created.getAdvanceFlag());
    }

    @Test
    @DisplayName("Create with an unknown loan fails with 404")
    void create_UnknownLoanFails() {
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> repaymentService.create(createRequest()));
    }

    @Test
    @DisplayName("Create propagates repository exceptions")
    void create_RepositoryThrows() {
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.of(loan));
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("REP-ABC123");
        when(repaymentRepository.save(any(RepaymentDetail.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> repaymentService.create(createRequest()));
    }

    @Test
    @DisplayName("Get by ID returns the DTO")
    void getById_ReturnsDto() {
        when(repaymentRepository.findById("REP-1")).thenReturn(Optional.of(existingRepayment()));

        RepaymentDetailDTO dto = repaymentService.getById("REP-1");

        assertEquals("REP-1", dto.getRepaymentId());
        assertEquals("LOAN-1", dto.getLoanId());
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404")
    void getById_UnknownFails() {
        when(repaymentRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> repaymentService.getById("NOPE"));
    }

    @Test
    @DisplayName("List without a filter queries all repayments")
    void list_AllReturnsPage() {
        RepaymentDetail repayment = existingRepayment();
        when(repaymentRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(repayment)));

        PageResponse<RepaymentDetailDTO> page = repaymentService.list(null, 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("REP-1", page.getContent().get(0).getRepaymentId());
        verify(repaymentRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List with a loan filter queries ordered schedule rows")
    void list_ByLoanId() {
        RepaymentDetail repayment = existingRepayment();
        when(repaymentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(repayment)));

        PageResponse<RepaymentDetailDTO> page = repaymentService.list("LOAN-1", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(repaymentRepository).findByLoan_LoanIdOrderByInstallmentNumberAsc(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(repaymentRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<RepaymentDetailDTO> page = repaymentService.list(null, 0, 10);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Delete removes the repayment row")
    void delete_Removes() {
        RepaymentDetail repayment = existingRepayment();
        when(repaymentRepository.findById("REP-1")).thenReturn(Optional.of(repayment));

        repaymentService.delete("REP-1");

        verify(repaymentRepository).delete(repayment);
    }

    @Test
    @DisplayName("Delete with an unknown ID fails with 404")
    void delete_UnknownFails() {
        when(repaymentRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> repaymentService.delete("NOPE"));
    }
}
