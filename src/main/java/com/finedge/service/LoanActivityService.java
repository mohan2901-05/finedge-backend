package com.finedge.service;

import com.finedge.dto.LoanActivityStatusCreateRequest;
import com.finedge.dto.LoanActivityStatusDTO;
import com.finedge.dto.PageResponse;
import com.finedge.entity.LoanActivityStatus;
import com.finedge.entity.LoanInfo;
import com.finedge.entity.UserInfo;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.LoanActivityStatusRepository;
import com.finedge.repository.LoanInfoRepository;
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
 * Loan stage history. Append-only by design: entries can be recorded and
 * read, but never updated or deleted through this API. Stage-code meanings
 * follow the schema comments (BDE/PDOC/DID/FINISH/REJ with A/C/D/S/R);
 * no transition rules are enforced.
 */
@Service
@Transactional
public class LoanActivityService {

    private final LoanActivityStatusRepository activityRepository;
    private final LoanInfoRepository loanRepository;
    private final UserInfoRepository userRepository;
    private final StringIdGenerator idGenerator;

    public LoanActivityService(LoanActivityStatusRepository activityRepository,
                               LoanInfoRepository loanRepository,
                               UserInfoRepository userRepository,
                               StringIdGenerator idGenerator) {
        this.activityRepository = activityRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanActivityStatusDTO> list(String loanId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedOn"));
        Page<LoanActivityStatus> result = (loanId == null || loanId.isBlank())
                ? activityRepository.findAll(pageable)
                : activityRepository.findByLoan_LoanId(loanId, pageable);
        List<LoanActivityStatusDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public LoanActivityStatusDTO getById(String id) {
        return mapToDTO(requireById(id));
    }

    public LoanActivityStatusDTO create(LoanActivityStatusCreateRequest request) {
        UserInfo user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getUserId()));
        LoanInfo loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan not found with id: " + request.getLoanId()));
        LoanActivityStatus activity = new LoanActivityStatus();
        activity.setActivityId(idGenerator.generateUnique("LAS", activityRepository::existsById));
        activity.setUser(user);
        activity.setLoan(loan);
        activity.setActivityCode(request.getActivityCode().trim());
        activity.setStatusCode(request.getStatusCode().trim());
        return mapToDTO(activityRepository.save(activity));
    }

    public LoanActivityStatusDTO mapToDTO(LoanActivityStatus activity) {
        LoanActivityStatusDTO dto = new LoanActivityStatusDTO();
        dto.setActivityId(activity.getActivityId());
        if (activity.getUser() != null) {
            dto.setUserId(activity.getUser().getUserId());
        }
        if (activity.getLoan() != null) {
            dto.setLoanId(activity.getLoan().getLoanId());
        }
        dto.setActivityCode(activity.getActivityCode());
        dto.setStatusCode(activity.getStatusCode());
        dto.setUpdatedOn(activity.getUpdatedOn());
        return dto;
    }

    private LoanActivityStatus requireById(String id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan activity not found with id: " + id));
    }
}
