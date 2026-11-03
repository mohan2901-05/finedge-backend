package com.finedge.service;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RepaymentDetailCreateRequest;
import com.finedge.dto.RepaymentDetailDTO;
import com.finedge.entity.LoanInfo;
import com.finedge.entity.RepaymentDetail;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.LoanInfoRepository;
import com.finedge.repository.RepaymentDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Amortisation schedule rows. Rows are stored exactly as supplied — no
 * schedule generation or EMI maths lives here. Schedule rows are immutable
 * once posted (no update endpoint); corrections are made by deleting and
 * re-creating rows.
 */
@Service
@Transactional
public class RepaymentService {

    private final RepaymentDetailRepository repaymentRepository;
    private final LoanInfoRepository loanRepository;
    private final StringIdGenerator idGenerator;

    public RepaymentService(RepaymentDetailRepository repaymentRepository,
                            LoanInfoRepository loanRepository,
                            StringIdGenerator idGenerator) {
        this.repaymentRepository = repaymentRepository;
        this.loanRepository = loanRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<RepaymentDetailDTO> list(String loanId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RepaymentDetail> result = (loanId == null || loanId.isBlank())
                ? repaymentRepository.findAll(pageable)
                : repaymentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(loanId, pageable);
        List<RepaymentDetailDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public RepaymentDetailDTO getById(String id) {
        return mapToDTO(requireById(id));
    }

    public RepaymentDetailDTO create(RepaymentDetailCreateRequest request) {
        LoanInfo loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan not found with id: " + request.getLoanId()));
        RepaymentDetail repayment = new RepaymentDetail();
        repayment.setRepaymentId(idGenerator.generateUnique("REP", repaymentRepository::existsById));
        repayment.setLoan(loan);
        repayment.setInstallmentNumber(request.getInstallmentNumber());
        repayment.setDueDate(request.getDueDate());
        repayment.setOpeningPrincipal(request.getOpeningPrincipal());
        repayment.setInstallmentAmount(request.getInstallmentAmount());
        repayment.setPrincipal(request.getPrincipal());
        repayment.setInterest(request.getInterest());
        repayment.setAdvanceFlag(request.getAdvanceFlag() != null ? request.getAdvanceFlag() : "N");
        repayment.setClosingPrincipal(request.getClosingPrincipal());
        repayment.setRate(request.getRate());
        return mapToDTO(repaymentRepository.save(repayment));
    }

    public void delete(String id) {
        repaymentRepository.delete(requireById(id));
    }

    public RepaymentDetailDTO mapToDTO(RepaymentDetail repayment) {
        RepaymentDetailDTO dto = new RepaymentDetailDTO();
        dto.setRepaymentId(repayment.getRepaymentId());
        if (repayment.getLoan() != null) {
            dto.setLoanId(repayment.getLoan().getLoanId());
        }
        dto.setInstallmentNumber(repayment.getInstallmentNumber());
        dto.setDueDate(repayment.getDueDate());
        dto.setOpeningPrincipal(repayment.getOpeningPrincipal());
        dto.setInstallmentAmount(repayment.getInstallmentAmount());
        dto.setPrincipal(repayment.getPrincipal());
        dto.setInterest(repayment.getInterest());
        dto.setAdvanceFlag(repayment.getAdvanceFlag());
        dto.setClosingPrincipal(repayment.getClosingPrincipal());
        dto.setRate(repayment.getRate());
        dto.setCreatedAt(repayment.getCreatedAt());
        return dto;
    }

    private RepaymentDetail requireById(String id) {
        return repaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repayment not found with id: " + id));
    }
}
