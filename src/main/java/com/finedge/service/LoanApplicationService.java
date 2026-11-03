package com.finedge.service;

import com.finedge.dto.LoanApplicationCreateRequest;
import com.finedge.dto.LoanApplicationDTO;
import com.finedge.dto.LoanApplicationUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.Financier;
import com.finedge.entity.LoanApplication;
import com.finedge.exception.BusinessRuleException;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.FinancierRepository;
import com.finedge.repository.LoanApplicationRepository;
import com.finedge.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Loan intake staging records. Part D4: every application is associated
 * with the authenticated borrower who created it. Financial figures
 * (EMI, total interest, total payable) are calculated on the server and
 * client-supplied values are never trusted. Financier references are
 * validated against the authoritative {@code financiers} table. Application
 * references are generated on the server.
 *
 * <p>All read/write operations are scoped to the current user's own
 * applications; a borrower cannot access another borrower's application.
 * Borrower ownership cannot be changed or cleared by updates.
 */
@Service
@Transactional
public class LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final FinancierRepository financierRepository;
    private final EmiCalculationService emiCalculationService;
    private final StringIdGenerator idGenerator;
    private final CurrentUserService currentUserService;

    public LoanApplicationService(LoanApplicationRepository applicationRepository,
                                  FinancierRepository financierRepository,
                                  EmiCalculationService emiCalculationService,
                                  StringIdGenerator idGenerator,
                                  CurrentUserService currentUserService) {
        this.applicationRepository = applicationRepository;
        this.financierRepository = financierRepository;
        this.emiCalculationService = emiCalculationService;
        this.idGenerator = idGenerator;
        this.currentUserService = currentUserService;
    }

    /**
     * Returns the ID of the currently authenticated borrower, or throws if
     * no user is authenticated. All loan-application endpoints require
     * authentication, so this should always succeed for authenticated
     * requests.
     */
    private String requireCurrentUserId() {
        return currentUserService.getCurrentUserId()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No authenticated borrower found. Loan applications require an authenticated user."));
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanApplicationDTO> list(String status, int page, int size) {
        String custUserId = requireCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LoanApplication> result;
        if (status == null || status.isBlank()) {
            result = applicationRepository.findByBorrower_UserId(custUserId, pageable);
        } else {
            result = applicationRepository.findByBorrower_UserIdAndStatus(custUserId, status, pageable);
        }
        List<LoanApplicationDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public LoanApplicationDTO getById(Integer id) {
        String custUserId = requireCurrentUserId();
        return mapToDTO(requireByIdAndBorrower(id, custUserId));
    }

    @Transactional(readOnly = true)
    public LoanApplicationDTO getByRef(String ref) {
        String custUserId = requireCurrentUserId();
        LoanApplication application = applicationRepository.findByRefAndBorrower_UserId(ref, custUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan application not found with ref: " + ref));
        return mapToDTO(application);
    }

    /**
     * Returns the financier key of the currently authenticated user, or throws
     * if the user has no financier association. Used to scope financier
     * read/write operations to the user's own institution.
     */
    private String requireCurrentFinancierKey() {
        return currentUserService.getCurrentUser()
                .map(user -> user.getFinancier() != null ? user.getFinancier().getFinancierId() : null)
                .filter(java.util.Objects::nonNull)
                .filter(key -> !key.isBlank())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No financier association found for the current user. " +
                                "Financier review operations require an associated financier institution."));
    }

    /**
     * Lists the current financier's own applications, financier-scoped by the
     * authenticated user's associated financier institution.
     */
    @Transactional(readOnly = true)
    public PageResponse<LoanApplicationDTO> listForFinancier(String status, int page, int size) {
        String financierKey = requireCurrentFinancierKey();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LoanApplication> result;
        if (status == null || status.isBlank()) {
            result = applicationRepository.findByFinancierKey(financierKey, pageable);
        } else {
            result = applicationRepository.findByFinancierKeyAndStatus(financierKey, status, pageable);
        }
        List<LoanApplicationDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    /**
     * Gets a single application by ID, financier-scoped by the authenticated
     * user's associated financier institution. Uses 404 (not 403) to avoid
     * leaking whether the application exists.
     */
    @Transactional(readOnly = true)
    public LoanApplicationDTO getByIdForFinancier(Integer id) {
        String financierKey = requireCurrentFinancierKey();
        return mapToDTO(requireByIdAndFinancier(id, financierKey));
    }

    /**
     * Approves a {@code Pending Review} application owned by the current
     * financier. Rejects invalid transitions (e.g. already decided).
     */
    public LoanApplicationDTO approve(Integer id) {
        String financierKey = requireCurrentFinancierKey();
        LoanApplication application = requireByIdAndFinancier(id, financierKey);
        if (!"Pending Review".equals(application.getStatus())) {
            throw new BusinessRuleException(
                    "Application '" + application.getRef() + "' is not in 'Pending Review' status: " +
                            application.getStatus());
        }
        application.setStatus("Approved");
        return mapToDTO(applicationRepository.save(application));
    }

    /**
     * Rejects a {@code Pending Review} application owned by the current
     * financier. Rejects invalid transitions (e.g. already decided).
     */
    public LoanApplicationDTO reject(Integer id) {
        String financierKey = requireCurrentFinancierKey();
        LoanApplication application = requireByIdAndFinancier(id, financierKey);
        if (!"Pending Review".equals(application.getStatus())) {
            throw new BusinessRuleException(
                    "Application '" + application.getRef() + "' is not in 'Pending Review' status: " +
                            application.getStatus());
        }
        application.setStatus("Rejected");
        return mapToDTO(applicationRepository.save(application));
    }

    public LoanApplicationDTO create(LoanApplicationCreateRequest request) {
        String custUserId = requireCurrentUserId();

        // Validate and resolve the borrower from the security context.
        // A client-supplied borrower ID can never override this.
        var borrower = currentUserService.getCurrentUser()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated borrower could not be resolved."));

        // Generate reference on the server. Client-supplied references are
        // rejected by the absence of the ref field in the create request.
        String ref = idGenerator.generateUnique("LNA", applicationRepository::existsByRef);

        // Validate the financier: must exist and be ACTIVE.
        Financier financier = financierRepository.findById(request.getFinancierKey().trim())
                .orElseThrow(() -> new BusinessRuleException(
                        "Financier '" + request.getFinancierKey() + "' does not exist."));
        if (!"ACTIVE".equalsIgnoreCase(financier.getStatus())) {
            throw new BusinessRuleException(
                    "Financier '" + request.getFinancierKey() + "' is not active.");
        }

        // Calculate financial figures on the server. Client-supplied EMI,
        // total interest, and total payable are never trusted.
        EmiCalculationService.EmiCalculationResult calc = emiCalculationService.calculate(
                request.getLoanAmount(), request.getInterestRate(), request.getTenureMonths());

        LoanApplication application = new LoanApplication();
        application.setRef(ref);
        application.setCustomerName(request.getCustomerName().trim());
        application.setCustomerPhone(request.getCustomerPhone().trim());
        application.setCustomerEmail(request.getCustomerEmail().trim());
        application.setCustomerDob(request.getCustomerDob());
        application.setCustomerPan(request.getCustomerPan());
        application.setCustomerCity(request.getCustomerCity().trim());
        application.setCustomerAddress(request.getCustomerAddress().trim());
        // Use the financier key as the authoritative reference.
        application.setFinancierKey(financier.getFinancierId());
        // Derive the financier label from the authoritative database record,
        // not from client input.
        application.setFinancierLabel(financier.getInstitutionName());
        application.setLoanAmount(request.getLoanAmount());
        application.setTenureMonths(request.getTenureMonths());
        application.setInterestRate(request.getInterestRate());
        application.setEmi(calc.emi());
        application.setTotalInterest(calc.totalInterest());
        application.setTotalPayable(calc.totalPayable());
        application.setStatus(request.getStatus() != null ? request.getStatus() : "Pending Review");
        application.setBorrower(borrower);
        return mapToDTO(applicationRepository.save(application));
    }

    public LoanApplicationDTO update(Integer id, LoanApplicationUpdateRequest request) {
        String custUserId = requireCurrentUserId();
        LoanApplication application = requireByIdAndBorrower(id, custUserId);

        // Borrower ownership is preserved — it cannot be changed or cleared
        // by an update request. The update DTO has no borrower field.

        boolean recalculate = false;
        if (request.getCustomerName() != null) {
            application.setCustomerName(request.getCustomerName().trim());
        }
        if (request.getCustomerPhone() != null) {
            application.setCustomerPhone(request.getCustomerPhone().trim());
        }
        if (request.getCustomerEmail() != null) {
            application.setCustomerEmail(request.getCustomerEmail().trim());
        }
        if (request.getCustomerDob() != null) {
            application.setCustomerDob(request.getCustomerDob());
        }
        if (request.getCustomerCity() != null) {
            application.setCustomerCity(request.getCustomerCity().trim());
        }
        if (request.getCustomerAddress() != null) {
            application.setCustomerAddress(request.getCustomerAddress().trim());
        }
        if (request.getFinancierKey() != null) {
            // Validate the financier on update as well.
            Financier financier = financierRepository.findById(request.getFinancierKey().trim())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Financier '" + request.getFinancierKey() + "' does not exist."));
            if (!"ACTIVE".equalsIgnoreCase(financier.getStatus())) {
                throw new BusinessRuleException(
                        "Financier '" + request.getFinancierKey() + "' is not active.");
            }
            application.setFinancierKey(financier.getFinancierId());
            application.setFinancierLabel(financier.getInstitutionName());
        }
        if (request.getLoanAmount() != null) {
            application.setLoanAmount(request.getLoanAmount());
            recalculate = true;
        }
        if (request.getTenureMonths() != null) {
            application.setTenureMonths(request.getTenureMonths());
            recalculate = true;
        }
        if (request.getInterestRate() != null) {
            application.setInterestRate(request.getInterestRate());
            recalculate = true;
        }
        if (recalculate) {
            EmiCalculationService.EmiCalculationResult calc = emiCalculationService.calculate(
                    application.getLoanAmount(), application.getInterestRate(),
                    application.getTenureMonths());
            application.setEmi(calc.emi());
            application.setTotalInterest(calc.totalInterest());
            application.setTotalPayable(calc.totalPayable());
        }
        // Client-supplied emi, totalInterest, totalPayable are absent from
        // the update DTO and are never trusted.
        // Status transitions are intentionally NOT allowed via the borrower
        // update endpoint: approval/rejection is exclusively handled by the
        // financier review endpoints ({@link #approve}, {@link #reject}),
        // which enforce the Pending Review guard.
        return mapToDTO(applicationRepository.save(application));
    }

    public void delete(Integer id) {
        String custUserId = requireCurrentUserId();
        applicationRepository.delete(requireByIdAndBorrower(id, custUserId));
    }

    public LoanApplicationDTO mapToDTO(LoanApplication application) {
        LoanApplicationDTO dto = new LoanApplicationDTO();
        dto.setId(application.getId());
        dto.setRef(application.getRef());
        if (application.getBorrower() != null) {
            dto.setCustUserId(application.getBorrower().getUserId());
        }
        dto.setCustomerName(application.getCustomerName());
        dto.setCustomerPhone(application.getCustomerPhone());
        dto.setCustomerEmail(application.getCustomerEmail());
        dto.setCustomerDob(application.getCustomerDob());
        dto.setCustomerCity(application.getCustomerCity());
        dto.setCustomerAddress(application.getCustomerAddress());
        dto.setFinancierKey(application.getFinancierKey());
        dto.setFinancierLabel(application.getFinancierLabel());
        dto.setLoanAmount(application.getLoanAmount());
        dto.setTenureMonths(application.getTenureMonths());
        dto.setInterestRate(application.getInterestRate());
        dto.setEmi(application.getEmi());
        dto.setTotalInterest(application.getTotalInterest());
        dto.setTotalPayable(application.getTotalPayable());
        dto.setStatus(application.getStatus());
        dto.setCreatedAt(application.getCreatedAt());
        return dto;
    }

    /**
     * Finds an application by ID and borrower user ID, throwing
     * {@link ResourceNotFoundException} if not found or not owned by the
     * borrower. Using 404 (not 403) avoids leaking whether the application
     * exists, consistent with existing API conventions.
     */
    private LoanApplication requireByIdAndBorrower(Integer id, String custUserId) {
        return applicationRepository.findByIdAndBorrower_UserId(id, custUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));
    }

    /**
     * Finds an application by ID and financier key, throwing
     * {@link ResourceNotFoundException} if not found or not owned by the
     * financier. Using 404 (not 403) avoids leaking whether the application
     * exists, consistent with existing API conventions.
     */
    private LoanApplication requireByIdAndFinancier(Integer id, String financierKey) {
        return applicationRepository.findByIdAndFinancierKey(id, financierKey)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));
    }
}
