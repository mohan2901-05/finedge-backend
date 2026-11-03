package com.finedge.service;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RolePermissionMappingCreateRequest;
import com.finedge.dto.RolePermissionMappingDTO;
import com.finedge.entity.ModuleMaster;
import com.finedge.entity.Organization;
import com.finedge.entity.PermissionMaster;
import com.finedge.entity.RoleMaster;
import com.finedge.entity.RolePermissionMapping;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.ModuleMasterRepository;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.PermissionMasterRepository;
import com.finedge.repository.RoleMasterRepository;
import com.finedge.repository.RolePermissionMappingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Role-permission assignments. This service records which permissions a
 * role holds — it does not enforce them. No authentication, authorization
 * check, or request filtering exists in this application (see report).
 */
@Service
@Transactional
public class RolePermissionMappingService {

    private final RolePermissionMappingRepository mappingRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleMasterRepository roleRepository;
    private final ModuleMasterRepository moduleRepository;
    private final PermissionMasterRepository permissionRepository;
    private final StringIdGenerator idGenerator;

    public RolePermissionMappingService(RolePermissionMappingRepository mappingRepository,
                                        OrganizationRepository organizationRepository,
                                        RoleMasterRepository roleRepository,
                                        ModuleMasterRepository moduleRepository,
                                        PermissionMasterRepository permissionRepository,
                                        StringIdGenerator idGenerator) {
        this.mappingRepository = mappingRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.moduleRepository = moduleRepository;
        this.permissionRepository = permissionRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<RolePermissionMappingDTO> list(String orgId, String roleId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedDatetime"));
        Page<RolePermissionMapping> result;
        if (roleId != null && !roleId.isBlank()) {
            result = mappingRepository.findByRole_RoleId(roleId, pageable);
        } else if (orgId != null && !orgId.isBlank()) {
            result = mappingRepository.findByOrganization_OrgId(orgId, pageable);
        } else {
            result = mappingRepository.findAll(pageable);
        }
        List<RolePermissionMappingDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public RolePermissionMappingDTO getById(String id) {
        return mapToDTO(requireById(id));
    }

    public RolePermissionMappingDTO create(RolePermissionMappingCreateRequest request) {
        Organization org = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found with id: " + request.getOrgId()));
        RoleMaster role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role not found with id: " + request.getRoleId()));
        ModuleMaster module = moduleRepository.findById(request.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Module not found with id: " + request.getModuleId()));
        PermissionMaster permission = permissionRepository.findById(request.getPermissionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permission not found with id: " + request.getPermissionId()));
        if (mappingRepository.existsByOrganization_OrgIdAndRole_RoleIdAndModule_ModuleIdAndPermission_PermissionId(
                request.getOrgId(), request.getRoleId(), request.getModuleId(), request.getPermissionId())) {
            throw new DuplicateResourceException(
                    "This organization/role/module/permission assignment already exists.");
        }
        RolePermissionMapping mapping = new RolePermissionMapping();
        mapping.setMappingId(idGenerator.generateUnique("MAP", mappingRepository::existsById));
        mapping.setOrganization(org);
        mapping.setRole(role);
        mapping.setModule(module);
        mapping.setPermission(permission);
        return mapToDTO(mappingRepository.save(mapping));
    }

    /**
     * Revokes the assignment (deletes the mapping row only).
     */
    public void delete(String id) {
        mappingRepository.delete(requireById(id));
    }

    public RolePermissionMappingDTO mapToDTO(RolePermissionMapping mapping) {
        RolePermissionMappingDTO dto = new RolePermissionMappingDTO();
        dto.setMappingId(mapping.getMappingId());
        if (mapping.getOrganization() != null) {
            dto.setOrgId(mapping.getOrganization().getOrgId());
        }
        if (mapping.getRole() != null) {
            dto.setRoleId(mapping.getRole().getRoleId());
        }
        if (mapping.getModule() != null) {
            dto.setModuleId(mapping.getModule().getModuleId());
            dto.setModuleCode(mapping.getModule().getModuleCode());
        }
        if (mapping.getPermission() != null) {
            dto.setPermissionId(mapping.getPermission().getPermissionId());
            dto.setPermissionCode(mapping.getPermission().getPermissionCode());
        }
        dto.setCreatedDatetime(mapping.getCreatedDatetime());
        dto.setUpdatedDatetime(mapping.getUpdatedDatetime());
        return dto;
    }

    private RolePermissionMapping requireById(String id) {
        return mappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role-permission mapping not found with id: " + id));
    }
}
