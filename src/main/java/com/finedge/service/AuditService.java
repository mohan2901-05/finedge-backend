package com.finedge.service;

import com.finedge.dto.AuditLogCreateRequest;
import com.finedge.dto.AuditLogDTO;
import com.finedge.dto.PageResponse;
import com.finedge.entity.AuditLog;
import com.finedge.entity.Organization;
import com.finedge.entity.UserInfo;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.AuditLogRepository;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.UserInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Audit trail. Append-only by design: entries can be recorded and read,
 * but never updated or deleted through this API.
 */
@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditRepository;
    private final OrganizationRepository organizationRepository;
    private final UserInfoRepository userRepository;
    private final StringIdGenerator idGenerator;

    public AuditService(AuditLogRepository auditRepository,
                        OrganizationRepository organizationRepository,
                        UserInfoRepository userRepository,
                        StringIdGenerator idGenerator) {
        this.auditRepository = auditRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> list(String orgId, String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> result;
        if (orgId != null && !orgId.isBlank()) {
            result = auditRepository.findByOrganization_OrgIdOrderByCreatedDatetimeDesc(orgId, pageable);
        } else if (userId != null && !userId.isBlank()) {
            result = auditRepository.findByUser_UserIdOrderByCreatedDatetimeDesc(userId, pageable);
        } else {
            result = auditRepository.findAll(pageable);
        }
        List<AuditLogDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public AuditLogDTO getById(String id) {
        return mapToDTO(requireById(id));
    }

    public AuditLogDTO create(AuditLogCreateRequest request) {
        Organization org = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found with id: " + request.getOrgId()));
        UserInfo user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getUserId()));
        AuditLog audit = new AuditLog();
        audit.setAuditId(idGenerator.generateUnique("AUD", auditRepository::existsById));
        audit.setOrganization(org);
        audit.setUser(user);
        audit.setUserName(request.getUserName().trim());
        audit.setAction(request.getAction().trim());
        audit.setDetails(request.getDetails());
        return mapToDTO(auditRepository.save(audit));
    }

    public AuditLogDTO mapToDTO(AuditLog audit) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setAuditId(audit.getAuditId());
        if (audit.getOrganization() != null) {
            dto.setOrgId(audit.getOrganization().getOrgId());
        }
        if (audit.getUser() != null) {
            dto.setUserId(audit.getUser().getUserId());
        }
        dto.setUserName(audit.getUserName());
        dto.setAction(audit.getAction());
        dto.setDetails(audit.getDetails());
        dto.setCreatedDatetime(audit.getCreatedDatetime());
        return dto;
    }

    private AuditLog requireById(String id) {
        return auditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit entry not found with id: " + id));
    }
}
