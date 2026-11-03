package com.finedge.service;

import com.finedge.dto.FinancierProfile;
import com.finedge.dto.FinancierProfileCreateRequest;
import com.finedge.dto.FinancierProfileUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.Financier;
import com.finedge.exception.BusinessRuleException;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.FinancierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * M2 Part C service for the financier directory.
 *
 * <p>Public reads (list, getById) return only {@code ACTIVE} financiers by
 * default. Writes (create, update, deactivate) require the
 * {@code ROLE_FINANCIER_ADMIN} role, enforced via {@link PreAuthorize} on
 * the controller layer.
 *
 * <p>Soft deactivation: {@code deactivate} sets {@code status} to
 * {@code INACTIVE} rather than deleting the row, preserving any historical
 * references.
 */
@Service
@Transactional
public class FinancierService {

    private final FinancierRepository financierRepository;
    private final StringIdGenerator idGenerator;

    public FinancierService(FinancierRepository financierRepository,
                            StringIdGenerator idGenerator) {
        this.financierRepository = financierRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancierProfile> list(String status, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "institutionName"));
        Page<Financier> result;
        if (search != null && !search.isBlank()) {
            result = financierRepository.findByInstitutionNameContainingIgnoreCase(search, pageable);
        } else if (status != null && !status.isBlank()) {
            result = financierRepository.findByStatus(status, pageable);
        } else {
            result = financierRepository.findByStatus("ACTIVE", pageable);
        }
        List<FinancierProfile> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public FinancierProfile getById(String id) {
        return mapToDTO(requireById(id));
    }

    @PreAuthorize("hasAuthority('ROLE_FINANCIER_ADMIN')")
    public FinancierProfile create(FinancierProfileCreateRequest request) {
        if (financierRepository.existsByInstitutionName(request.getInstitutionName().trim())) {
            throw new DuplicateResourceException(
                    "A financier with institution name '" + request.getInstitutionName().trim() + "' already exists.");
        }
        Financier financier = new Financier();
        financier.setFinancierId(idGenerator.generateUnique("FIN", financierRepository::existsById));
        financier.setInstitutionName(request.getInstitutionName().trim());
        financier.setDescription(request.getDescription());
        financier.setIndicativeAnnualRate(request.getIndicativeAnnualRate());
        financier.setMinLoanAmount(request.getMinLoanAmount());
        financier.setMaxLoanAmount(request.getMaxLoanAmount());
        financier.setAvailableTenureMonths(request.getAvailableTenureMonths());
        financier.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        validateCreate(financier);
        return mapToDTO(financierRepository.save(financier));
    }

    @PreAuthorize("hasAuthority('ROLE_FINANCIER_ADMIN')")
    public FinancierProfile update(String id, FinancierProfileUpdateRequest request) {
        Financier financier = requireById(id);
        if (request.getInstitutionName() != null) {
            String name = request.getInstitutionName().trim();
            if (!name.equalsIgnoreCase(financier.getInstitutionName())
                    && financierRepository.existsByInstitutionName(name)) {
                throw new DuplicateResourceException(
                        "A financier with institution name '" + name + "' already exists.");
            }
            financier.setInstitutionName(name);
        }
        if (request.getDescription() != null) {
            financier.setDescription(request.getDescription());
        }
        if (request.getIndicativeAnnualRate() != null) {
            financier.setIndicativeAnnualRate(request.getIndicativeAnnualRate());
        }
        if (request.getMinLoanAmount() != null) {
            financier.setMinLoanAmount(request.getMinLoanAmount());
        }
        if (request.getMaxLoanAmount() != null) {
            financier.setMaxLoanAmount(request.getMaxLoanAmount());
        }
        if (request.getAvailableTenureMonths() != null) {
            financier.setAvailableTenureMonths(request.getAvailableTenureMonths());
        }
        if (request.getStatus() != null) {
            financier.setStatus(request.getStatus());
        }
        validateUpdate(financier);
        return mapToDTO(financierRepository.save(financier));
    }

    @PreAuthorize("hasAuthority('ROLE_FINANCIER_ADMIN')")
    public void deactivate(String id) {
        Financier financier = requireById(id);
        financier.setStatus("INACTIVE");
        financierRepository.save(financier);
    }

    public FinancierProfile mapToDTO(Financier financier) {
        FinancierProfile dto = new FinancierProfile();
        dto.setFinancierId(financier.getFinancierId());
        dto.setInstitutionName(financier.getInstitutionName());
        dto.setDescription(financier.getDescription());
        dto.setIndicativeAnnualRate(financier.getIndicativeAnnualRate());
        dto.setMinLoanAmount(financier.getMinLoanAmount());
        dto.setMaxLoanAmount(financier.getMaxLoanAmount());
        dto.setAvailableTenureMonths(financier.getAvailableTenureMonths());
        dto.setStatus(financier.getStatus());
        dto.setCreatedDatetime(financier.getCreatedDatetime());
        dto.setUpdatedDatetime(financier.getUpdatedDatetime());
        return dto;
    }

    private Financier requireById(String id) {
        return financierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financier not found with id: " + id));
    }

    /**
     * Validates a newly created financier: status must be ACTIVE or INACTIVE,
     * and min loan amount must not exceed max loan amount when both are set.
     */
    private void validateCreate(Financier financier) {
        validateStatus(financier.getStatus());
        validateAmountRange(financier.getMinLoanAmount(), financier.getMaxLoanAmount());
    }

    /**
     * Validates an updated financier: status must be ACTIVE or INACTIVE when
     * present, and min loan amount must not exceed max loan amount when both
     * are set.
     */
    private void validateUpdate(Financier financier) {
        validateStatus(financier.getStatus());
        validateAmountRange(financier.getMinLoanAmount(), financier.getMaxLoanAmount());
    }

    private void validateStatus(String status) {
        if (status != null && !"ACTIVE".equalsIgnoreCase(status) && !"INACTIVE".equalsIgnoreCase(status)) {
            throw new BusinessRuleException(
                    "Invalid status '" + status + "': must be ACTIVE or INACTIVE.");
        }
    }

    private void validateAmountRange(BigDecimal minLoanAmount, BigDecimal maxLoanAmount) {
        if (minLoanAmount != null && maxLoanAmount != null && minLoanAmount.compareTo(maxLoanAmount) > 0) {
            throw new BusinessRuleException(
                    "Minimum loan amount " + minLoanAmount
                            + " must not exceed maximum loan amount " + maxLoanAmount + ".");
        }
    }
}
