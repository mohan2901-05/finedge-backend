package com.finedge.service;

import com.finedge.dto.DisbursementCreateRequest;
import com.finedge.dto.DisbursementDTO;
import com.finedge.dto.OutstandingDTO;
import com.finedge.dto.PageResponse;
import com.finedge.dto.RepaymentDTO;
import com.finedge.entity.LoanApplication;
import com.finedge.entity.LoanDisbursement;
import com.finedge.entity.LoanRepayment;
import com.finedge.exception.BusinessRuleException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.LoanApplicationRepository;
import com.finedge.repository.LoanDisbursementRepository;
import com.finedge.repository.LoanRepaymentRepository;
import com.finedge.security.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Disbursement and outstanding-balance operations for loan applications
 * (Roadmap G).
 *
 * <p>Only an {@code Approved} loan application may be disbursed, only by a
 * financier scoped to the application's own institution, and only once.
 * The disbursed amount and timestamp are persisted. The outstanding balance
 * is computed on the backend from the disbursed amount minus the sum of
 * persisted repayments; no balance column is stored.
 *
 * <p>Money uses {@link BigDecimal} with a fixed scale of 2 and {@link
 * RoundingMode#HALF_UP}. No interest allocation, penalty, or
 * installment-scheduling logic is implemented (none is defined by existing
 * requirements); this is a principal-balance workflow.
 */
@Service
@Transactional
public class LoanDisbursementService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private static final String APPROVED = "Approved";
    private static final String PENDING_REVIEW = "Pending Review";

    private final LoanApplicationRepository applicationRepository;
    private final LoanDisbursementRepository disbursementRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final StringIdGenerator idGenerator;
    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;

    public LoanDisbursementService(LoanApplicationRepository applicationRepository,
                                   LoanDisbursementRepository disbursementRepository,
                                   LoanRepaymentRepository repaymentRepository,
                                   StringIdGenerator idGenerator,
                                   CurrentUserService currentUserService,
                                   EntityManager entityManager) {
        this.applicationRepository = applicationRepository;
        this.disbursementRepository = disbursementRepository;
        this.repaymentRepository = repaymentRepository;
        this.idGenerator = idGenerator;
        this.currentUserService = currentUserService;
        this.entityManager = entityManager;
    }

    @Transactional
    public DisbursementDTO recordDisbursement(Integer applicationId, DisbursementCreateRequest request) {
        String financierKey = requireCurrentFinancierKey();
        LoanApplication application = requireByIdAndFinancier(applicationId, financierKey);

        if (!APPROVED.equals(application.getStatus())) {
            throw new BusinessRuleException(
                    "Application '" + application.getRef() + "' cannot be disbursed: status is '" +
                            application.getStatus() + "', only 'Approved' loans may be disbursed.");
        }

        // Serialize concurrent disbursement requests for the same application.
        entityManager.lock(application, LockModeType.PESSIMISTIC_WRITE);

        if (disbursementRepository.existsByApplicationId(applicationId)) {
            throw new BusinessRuleException(
                    "Application '" + application.getRef() + "' has already been disbursed.");
        }

        BigDecimal disbursedAmount = round(request.getDisbursedAmount());
        LocalDateTime now = LocalDateTime.now();

        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setDisbursementId(idGenerator.generateUnique("DISB",
                disbursementRepository::existsById));
        disbursement.setApplicationId(applicationId);
        disbursement.setDisbursedAmount(disbursedAmount);
        disbursement.setDisbursedAt(now);
        disbursement.setDisbursedByUserId(requireCurrentUserId());
        disbursement.setCreatedDatetime(now);

        return mapToDTO(disbursementRepository.save(disbursement), application.getLoanAmount());
    }

    @Transactional(readOnly = true)
    public DisbursementDTO getDisbursement(Integer applicationId) {
        LoanApplication application = scopeApplication(applicationId);
        LoanDisbursement disbursement = disbursementRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Disbursement not found for loan application id: " + applicationId));
        return mapToDTO(disbursement, application.getLoanAmount());
    }

    @Transactional(readOnly = true)
    public OutstandingDTO getOutstanding(Integer applicationId) {
        LoanApplication application = scopeApplication(applicationId);

        OutstandingDTO dto = new OutstandingDTO();
        dto.setApplicationId(applicationId);

        LoanDisbursement disbursement = disbursementRepository.findByApplicationId(applicationId).orElse(null);
        if (disbursement == null) {
            dto.setDisbursed(false);
            dto.setDisbursedAmount(BigDecimal.ZERO);
            dto.setTotalRepaid(BigDecimal.ZERO);
            dto.setOutstandingAmount(BigDecimal.ZERO);
            dto.setFullyRepaid(false);
            return dto;
        }

        BigDecimal totalRepaid = totalRepaid(applicationId);
        BigDecimal disbursedAmount = disbursement.getDisbursedAmount();
        BigDecimal outstanding = disbursedAmount.subtract(totalRepaid)
                .setScale(MONEY_SCALE, MONEY_ROUNDING);

        dto.setDisbursed(true);
        dto.setDisbursedAmount(disbursedAmount);
        dto.setTotalRepaid(totalRepaid);
        dto.setOutstandingAmount(outstanding);
        dto.setFullyRepaid(outstanding.compareTo(BigDecimal.ZERO) <= 0);
        return dto;
    }

    @Transactional(readOnly = true)
    public PageResponse<RepaymentDTO> listRepayments(Integer applicationId, int page, int size) {
        scopeApplication(applicationId);
        Pageable pageable = PageRequest.of(page, size);
        Page<LoanRepayment> result = repaymentRepository.findByApplicationIdOrderByPaidAtDesc(applicationId, pageable);
        List<RepaymentDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    private BigDecimal totalRepaid(Integer applicationId) {
        return repaymentRepository.findByApplicationIdOrderByPaidAtDesc(applicationId).stream()
                .map(LoanRepayment::getRepaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal round(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private DisbursementDTO mapToDTO(LoanDisbursement disbursement, BigDecimal loanAmount) {
        DisbursementDTO dto = new DisbursementDTO();
        dto.setDisbursementId(disbursement.getDisbursementId());
        dto.setApplicationId(disbursement.getApplicationId());
        dto.setDisbursedAmount(disbursement.getDisbursedAmount());
        dto.setLoanAmount(loanAmount);
        dto.setDisbursedAt(disbursement.getDisbursedAt());
        dto.setDisbursedByUserId(disbursement.getDisbursedByUserId());
        dto.setCreatedDatetime(disbursement.getCreatedDatetime());
        return dto;
    }

    private RepaymentDTO mapToDTO(LoanRepayment repayment) {
        RepaymentDTO dto = new RepaymentDTO();
        dto.setRepaymentId(repayment.getRepaymentId());
        dto.setApplicationId(repayment.getApplicationId());
        dto.setRepaymentAmount(repayment.getRepaymentAmount());
        dto.setPaidAt(repayment.getPaidAt());
        dto.setPaidByUserId(repayment.getPaidByUserId());
        dto.setCreatedDatetime(repayment.getCreatedDatetime());
        return dto;
    }

    private String requireCurrentUserId() {
        return currentUserService.getCurrentUserId()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No authenticated user found. Disbursement/repayment operations require an authenticated user."));
    }

    private String requireCurrentFinancierKey() {
        return currentUserService.getCurrentUser()
                .map(user -> user.getFinancier() != null ? user.getFinancier().getFinancierId() : null)
                .filter(java.util.Objects::nonNull)
                .filter(key -> !key.isBlank())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No financier association found for the current user. " +
                                "Financier disbursement operations require an associated financier institution."));
    }

    private boolean isFinancierAdmin() {
        return currentUserService.getCurrentUserRoles().contains("ROLE_FINANCIER_ADMIN");
    }

    /**
     * Resolves the application visible to the current user: financier-scoped
     * for {@code ROLE_FINANCIER_ADMIN}, borrower-scoped otherwise. Uses 404
     * (not 403) to avoid leaking existence, consistent with existing API
     * conventions.
     */
    private LoanApplication scopeApplication(Integer applicationId) {
        if (isFinancierAdmin()) {
            String financierKey = requireCurrentFinancierKey();
            return requireByIdAndFinancier(applicationId, financierKey);
        }
        String userId = requireCurrentUserId();
        return requireByIdAndBorrower(applicationId, userId);
    }

    private LoanApplication requireByIdAndBorrower(Integer id, String userId) {
        return applicationRepository.findByIdAndBorrower_UserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan application not found with id: " + id));
    }

    private LoanApplication requireByIdAndFinancier(Integer id, String financierKey) {
        return applicationRepository.findByIdAndFinancierKey(id, financierKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan application not found with id: " + id));
    }
}
