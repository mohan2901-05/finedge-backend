package com.finedge.service;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RoleCreateRequest;
import com.finedge.dto.RoleDTO;
import com.finedge.dto.RoleUpdateRequest;
import com.finedge.entity.Organization;
import com.finedge.entity.RoleMaster;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.RoleMasterRepository;
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
public class RoleService {

    private final RoleMasterRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final StringIdGenerator idGenerator;

    public RoleService(RoleMasterRepository roleRepository,
                       OrganizationRepository organizationRepository,
                       StringIdGenerator idGenerator) {
        this.roleRepository = roleRepository;
        this.organizationRepository = organizationRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<RoleDTO> list(String orgId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedDatetime"));
        Page<RoleMaster> result = (orgId == null || orgId.isBlank())
                ? roleRepository.findAll(pageable)
                : roleRepository.findByOrganization_OrgId(orgId, pageable);
        List<RoleDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public RoleDTO getById(String id) {
        return mapToDTO(requireById(id));
    }

    public RoleDTO create(RoleCreateRequest request) {
        Organization org = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found with id: " + request.getOrgId()));
        RoleMaster role = new RoleMaster();
        role.setRoleId(idGenerator.generateUnique("ROLE", roleRepository::existsById));
        role.setOrganization(org);
        role.setRoleName(request.getRoleName().trim());
        role.setRoleDescription(request.getRoleDescription());
        role.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        return mapToDTO(roleRepository.save(role));
    }

    public RoleDTO update(String id, RoleUpdateRequest request) {
        RoleMaster role = requireById(id);
        if (request.getRoleName() != null) {
            role.setRoleName(request.getRoleName().trim());
        }
        if (request.getRoleDescription() != null) {
            role.setRoleDescription(request.getRoleDescription());
        }
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        return mapToDTO(roleRepository.save(role));
    }

    /**
     * Deletes the role. Users, mappings, and dependent rows cascade at the
     * database level per the supplied schema.
     */
    public void delete(String id) {
        roleRepository.delete(requireById(id));
    }

    public RoleDTO mapToDTO(RoleMaster role) {
        RoleDTO dto = new RoleDTO();
        dto.setRoleId(role.getRoleId());
        if (role.getOrganization() != null) {
            dto.setOrgId(role.getOrganization().getOrgId());
            dto.setOrgName(role.getOrganization().getOrgName());
        }
        dto.setRoleName(role.getRoleName());
        dto.setRoleDescription(role.getRoleDescription());
        dto.setStatus(role.getStatus());
        dto.setCreatedDatetime(role.getCreatedDatetime());
        dto.setUpdatedDatetime(role.getUpdatedDatetime());
        return dto;
    }

    private RoleMaster requireById(String id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
    }
}
