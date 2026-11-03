package com.finedge.service;

import com.finedge.dto.LoanInfoCreateRequest;
import com.finedge.dto.LoanInfoDTO;
import com.finedge.dto.LoanInfoUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.LoanInfo;
import com.finedge.entity.Organization;
import com.finedge.entity.UserInfo;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.LoanInfoRepository;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.UserInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Booked loans. Persisted figures are stored exactly as supplied — this
 * service performs no balance, outstanding, EMI, or interest calculations.
 */
@Service
@Transactional
public class LoanInfoService {

    private final LoanInfoRepository loanRepository;
    private final OrganizationRepository organizationRepository;
    private final UserInfoRepository userRepository;
    private final StringIdGenerator idGenerator;

    public LoanInfoService(LoanInfoRepository loanRepository,
                           OrganizationRepository organizationRepository,
                           UserInfoRepository userRepository,
                           StringIdGenerator idGenerator) {
        this.loanRepository = loanRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanInfoDTO> list(String orgId, String userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedDatetime"));
        Page<LoanInfo> result;
        if (orgId != null && !orgId.isBlank()) {
            result = loanRepository.findByOrganization_OrgId(orgId, pageable);
        } else if (userId != null && !userId.isBlank()) {
            result = loanRepository.findByUser_UserId(userId, pageable);
        } else if (status != null && !status.isBlank()) {
            result = loanRepository.findByStatus(status, pageable);
        } else {
            result = loanRepository.findAll(pageable);
        }
        List<LoanInfoDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public LoanInfoDTO getById(String id) {
        return mapToDTO(requireById(id));
    }

    public LoanInfoDTO create(LoanInfoCreateRequest request) {
        Organization org = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found with id: " + request.getOrgId()));
        UserInfo user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getUserId()));
        LoanInfo loan = new LoanInfo();
        loan.setLoanId(idGenerator.generateUnique("LOAN", loanRepository::existsById));
        loan.setOrganization(org);
        loan.setUser(user);
        loan.setLoanAmount(request.getLoanAmount());
        loan.setInterestRate(request.getInterestRate());
        loan.setTenureMonths(request.getTenureMonths());
        loan.setPurpose(request.getPurpose());
        loan.setApplicationDate(request.getApplicationDate());
        loan.setApprovalDate(request.getApprovalDate());
        loan.setDisbursementDate(request.getDisbursementDate());
        loan.setDueDate(request.getDueDate());
        loan.setOutstandingAmount(request.getOutstandingAmount());
        if (request.getReceived() != null) {
            loan.setReceived(request.getReceived());
        }
        loan.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
        return mapToDTO(loanRepository.save(loan));
    }

    public LoanInfoDTO update(String id, LoanInfoUpdateRequest request) {
        LoanInfo loan = requireById(id);
        if (request.getPurpose() != null) {
            loan.setPurpose(request.getPurpose());
        }
        if (request.getApprovalDate() != null) {
            loan.setApprovalDate(request.getApprovalDate());
        }
        if (request.getDisbursementDate() != null) {
            loan.setDisbursementDate(request.getDisbursementDate());
        }
        if (request.getDueDate() != null) {
            loan.setDueDate(request.getDueDate());
        }
        if (request.getOutstandingAmount() != null) {
            loan.setOutstandingAmount(request.getOutstandingAmount());
        }
        if (request.getReceived() != null) {
            loan.setReceived(request.getReceived());
        }
        if (request.getStatus() != null) {
            loan.setStatus(request.getStatus());
        }
        return mapToDTO(loanRepository.save(loan));
    }

    /**
     * Deletes the loan. Its activity rows, repayment schedule, and
     * collections cascade at the database level per the supplied schema.
     */
    public void delete(String id) {
        loanRepository.delete(requireById(id));
    }

    public LoanInfoDTO mapToDTO(LoanInfo loan) {
        LoanInfoDTO dto = new LoanInfoDTO();
        dto.setLoanId(loan.getLoanId());
        if (loan.getOrganization() != null) {
            dto.setOrgId(loan.getOrganization().getOrgId());
        }
        if (loan.getUser() != null) {
            dto.setUserId(loan.getUser().getUserId());
        }
        dto.setLoanAmount(loan.getLoanAmount());
        dto.setInterestRate(loan.getInterestRate());
        dto.setTenureMonths(loan.getTenureMonths());
        dto.setPurpose(loan.getPurpose());
        dto.setApplicationDate(loan.getApplicationDate());
        dto.setApprovalDate(loan.getApprovalDate());
        dto.setDisbursementDate(loan.getDisbursementDate());
        dto.setDueDate(loan.getDueDate());
        dto.setOutstandingAmount(loan.getOutstandingAmount());
        dto.setReceived(loan.isReceived());
        dto.setStatus(loan.getStatus());
        dto.setCreatedDatetime(loan.getCreatedDatetime());
        dto.setUpdatedDatetime(loan.getUpdatedDatetime());
        return dto;
    }

    private LoanInfo requireById(String id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + id));
    }
}
