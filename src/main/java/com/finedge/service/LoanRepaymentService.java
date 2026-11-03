package com.finedge.service;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RepaymentCreateRequest;
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
 * Repayment operations for disbursed loan applications (Roadmap G).
 *
 * <p>A repayment may only be recorded against a successfully disbursed loan
 * application. Amounts are positive {@link BigDecimal} values and a
 * repayment must not exceed the outstanding balance. Each repayment is
 * persisted as an individual record with a stable identifier and timestamp.
 *
 * <p>Concurrent repayment requests are serialized with a pessimistic write
 * lock on the disbursement row so the outstanding balance is recomputed
 * before each repayment is applied, preventing overpayment.
 *
 * <p>Money uses {@link BigDecimal} with a fixed scale of 2 and {@link
 * RoundingMode#HALF_UP}. No interest allocation or penalty logic is
 * implemented (none is defined by existing requirements); this is a
 * principal-balance workflow.
 */
@Service
@Transactional
public class LoanRepaymentService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final LoanApplicationRepository applicationRepository;
    private final LoanDisbursementRepository disbursementRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final StringIdGenerator idGenerator;
    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;

    public LoanRepaymentService(LoanApplicationRepository applicationRepository,
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
    public RepaymentDTO recordRepayment(Integer applicationId, RepaymentCreateRequest request) {
        String userId = requireCurrentUserId();
        LoanApplication application = requireByIdAndBorrower(applicationId, userId);

        LoanDisbursement disbursement = disbursementRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Repayment cannot be recorded for application '" + application.getRef() +
                                "': the loan has not been disbursed."));

        // Serialize concurrent repayments against the same disbursement so
        // the outstanding balance is recomputed before each repayment.
        entityManager.lock(disbursement, LockModeType.PESSIMISTIC_WRITE);

        BigDecimal amount = round(request.getRepaymentAmount());
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Repayment amount must be greater than zero.");
        }

        BigDecimal totalRepaid = totalRepaid(applicationId);
        BigDecimal outstanding = disbursement.getDisbursedAmount().subtract(totalRepaid)
                .setScale(MONEY_SCALE, MONEY_ROUNDING);

        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessRuleException(
                    "Repayment amount " + amount + " exceeds the outstanding balance " + outstanding + ".");
        }

        LocalDateTime now = LocalDateTime.now();
        LoanRepayment repayment = new LoanRepayment();
        repayment.setRepaymentId(idGenerator.generateUnique("REPAY", repaymentRepository::existsById));
        repayment.setApplicationId(applicationId);
        repayment.setRepaymentAmount(amount);
        repayment.setPaidAt(now);
        repayment.setPaidByUserId(userId);
        repayment.setCreatedDatetime(now);

        return mapToDTO(repaymentRepository.save(repayment));
    }

    @Transactional(readOnly = true)
    public PageResponse<RepaymentDTO> listRepayments(Integer applicationId, int page, int size) {
        requireByIdAndBorrower(applicationId, requireCurrentUserId());
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
                        "No authenticated user found. Repayment operations require an authenticated user."));
    }

    private LoanApplication requireByIdAndBorrower(Integer id, String userId) {
        return applicationRepository.findByIdAndBorrower_UserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan application not found with id: " + id));
    }
}
