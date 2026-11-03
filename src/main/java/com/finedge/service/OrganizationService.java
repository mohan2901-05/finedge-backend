package com.finedge.service;

import com.finedge.dto.OrganizationCreateRequest;
import com.finedge.dto.OrganizationDTO;
import com.finedge.dto.OrganizationUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.Organization;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final StringIdGenerator idGenerator;

    public OrganizationService(OrganizationRepository organizationRepository,
                               StringIdGenerator idGenerator) {
        this.organizationRepository = organizationRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizationDTO> list(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedDatetime"));
        Page<Organization> result = (status == null || status.isBlank())
                ? organizationRepository.findAll(pageable)
                : organizationRepository.findByStatus(status, pageable);
        List<OrganizationDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public OrganizationDTO getById(String id) {
        return mapToDTO(requireById(id));
    }

    public OrganizationDTO create(OrganizationCreateRequest request) {
        if (organizationRepository.existsByOrgEmail(request.getOrgEmail())) {
            throw new DuplicateResourceException(
                    "An organization with email '" + request.getOrgEmail() + "' already exists.");
        }
        Organization org = new Organization();
        org.setOrgId(idGenerator.generateUnique("ORG", organizationRepository::existsById));
        org.setOrgName(request.getOrgName().trim());
        org.setOrgEmail(request.getOrgEmail().trim());
        org.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        return mapToDTO(organizationRepository.save(org));
    }

    public OrganizationDTO update(String id, OrganizationUpdateRequest request) {
        Organization org = requireById(id);
        if (request.getOrgName() != null) {
            org.setOrgName(request.getOrgName().trim());
        }
        if (request.getOrgEmail() != null) {
            String email = request.getOrgEmail().trim();
            if (!email.equalsIgnoreCase(org.getOrgEmail()) && organizationRepository.existsByOrgEmail(email)) {
                throw new DuplicateResourceException(
                        "An organization with email '" + email + "' already exists.");
            }
            org.setOrgEmail(email);
        }
        if (request.getStatus() != null) {
            org.setStatus(request.getStatus());
        }
        return mapToDTO(organizationRepository.save(org));
    }

    /**
     * Deletes the organization. The schema cascades to roles, users, loans,
     * collections, mappings, and audit rows — callers must confirm this
     * explicitly; there is no soft-delete or RESTRICT in the supplied schema.
     */
    public void delete(String id) {
        organizationRepository.delete(requireById(id));
    }

    public OrganizationDTO mapToDTO(Organization org) {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setOrgId(org.getOrgId());
        dto.setOrgName(org.getOrgName());
        dto.setOrgEmail(org.getOrgEmail());
        dto.setStatus(org.getStatus());
        dto.setCreatedDatetime(org.getCreatedDatetime());
        dto.setUpdatedDatetime(org.getUpdatedDatetime());
        return dto;
    }

    private Organization requireById(String id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
    }
}
