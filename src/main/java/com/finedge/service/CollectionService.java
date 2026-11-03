package com.finedge.service;

import com.finedge.dto.LoanCollectionCreateRequest;
import com.finedge.dto.LoanCollectionDTO;
import com.finedge.dto.LoanCollectionUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.LoanCollection;
import com.finedge.entity.LoanInfo;
import com.finedge.entity.Organization;
import com.finedge.entity.UserInfo;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.LoanCollectionRepository;
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
 * Cash-in records. Amounts are stored exactly as supplied — this service
 * performs no balance, outstanding, or reconciliation calculations.
 */
@Service
@Transactional
public class CollectionService {

    private final LoanCollectionRepository collectionRepository;
    private final OrganizationRepository organizationRepository;
    private final LoanInfoRepository loanRepository;
    private final UserInfoRepository userRepository;
    private final StringIdGenerator idGenerator;

    public CollectionService(LoanCollectionRepository collectionRepository,
                             OrganizationRepository organizationRepository,
                             LoanInfoRepository loanRepository,
                             UserInfoRepository userRepository,
                             StringIdGenerator idGenerator) {
        this.collectionRepository = collectionRepository;
        this.organizationRepository = organizationRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanCollectionDTO> list(String loanId, String orgId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedDatetime"));
        Page<LoanCollection> result;
        if (loanId != null && !loanId.isBlank()) {
            result = collectionRepository.findByLoan_LoanId(loanId, pageable);
        } else if (orgId != null && !orgId.isBlank()) {
            result = collectionRepository.findByOrganization_OrgId(orgId, pageable);
        } else if (status != null && !status.isBlank()) {
            result = collectionRepository.findByStatus(status, pageable);
        } else {
            result = collectionRepository.findAll(pageable);
        }
        List<LoanCollectionDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public LoanCollectionDTO getById(String id) {
        return mapToDTO(requireById(id));
    }

    public LoanCollectionDTO create(LoanCollectionCreateRequest request) {
        Organization org = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found with id: " + request.getOrgId()));
        LoanInfo loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan not found with id: " + request.getLoanId()));
        UserInfo payer = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getUserId()));
        UserInfo collector = null;
        if (request.getCollectedByUserId() != null && !request.getCollectedByUserId().isBlank()) {
            collector = userRepository.findById(request.getCollectedByUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Collecting user not found with id: " + request.getCollectedByUserId()));
        }
        LoanCollection collection = new LoanCollection();
        collection.setCollectionId(idGenerator.generateUnique("COLL", collectionRepository::existsById));
        collection.setOrganization(org);
        collection.setLoan(loan);
        collection.setUser(payer);
        collection.setCollectionAmount(request.getCollectionAmount());
        collection.setCollectionDate(request.getCollectionDate());
        collection.setPaymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : "CASH");
        collection.setReferenceNo(request.getReferenceNo());
        collection.setCollectedBy(collector);
        collection.setRemarks(request.getRemarks());
        collection.setStatus(request.getStatus() != null ? request.getStatus() : "SUCCESS");
        return mapToDTO(collectionRepository.save(collection));
    }

    public LoanCollectionDTO update(String id, LoanCollectionUpdateRequest request) {
        LoanCollection collection = requireById(id);
        if (request.getReferenceNo() != null) {
            collection.setReferenceNo(request.getReferenceNo());
        }
        if (request.getRemarks() != null) {
            collection.setRemarks(request.getRemarks());
        }
        if (request.getStatus() != null) {
            collection.setStatus(request.getStatus());
        }
        return mapToDTO(collectionRepository.save(collection));
    }

    public void delete(String id) {
        collectionRepository.delete(requireById(id));
    }

    public LoanCollectionDTO mapToDTO(LoanCollection collection) {
        LoanCollectionDTO dto = new LoanCollectionDTO();
        dto.setCollectionId(collection.getCollectionId());
        if (collection.getOrganization() != null) {
            dto.setOrgId(collection.getOrganization().getOrgId());
        }
        if (collection.getLoan() != null) {
            dto.setLoanId(collection.getLoan().getLoanId());
        }
        if (collection.getUser() != null) {
            dto.setUserId(collection.getUser().getUserId());
        }
        dto.setCollectionAmount(collection.getCollectionAmount());
        dto.setCollectionDate(collection.getCollectionDate());
        dto.setPaymentMode(collection.getPaymentMode());
        dto.setReferenceNo(collection.getReferenceNo());
        if (collection.getCollectedBy() != null) {
            dto.setCollectedByUserId(collection.getCollectedBy().getUserId());
        }
        dto.setRemarks(collection.getRemarks());
        dto.setStatus(collection.getStatus());
        dto.setCreatedDatetime(collection.getCreatedDatetime());
        dto.setUpdatedDatetime(collection.getUpdatedDatetime());
        return dto;
    }

    private LoanCollection requireById(String id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + id));
    }
}
