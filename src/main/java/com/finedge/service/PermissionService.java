package com.finedge.service;

import com.finedge.dto.PageResponse;
import com.finedge.dto.PermissionCreateRequest;
import com.finedge.dto.PermissionDTO;
import com.finedge.dto.PermissionUpdateRequest;
import com.finedge.entity.PermissionMaster;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.PermissionMasterRepository;
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
public class PermissionService {

    private final PermissionMasterRepository permissionRepository;
    private final StringIdGenerator idGenerator;

    public PermissionService(PermissionMasterRepository permissionRepository,
                             StringIdGenerator idGenerator) {
        this.permissionRepository = permissionRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<PermissionDTO> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedDatetime"));
        Page<PermissionMaster> result = permissionRepository.findAll(pageable);
        List<PermissionDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public PermissionDTO getById(String id) {
        return mapToDTO(requireById(id));
    }

    public PermissionDTO create(PermissionCreateRequest request) {
        if (permissionRepository.existsByPermissionCode(request.getPermissionCode())) {
            throw new DuplicateResourceException(
                    "A permission with code '" + request.getPermissionCode() + "' already exists.");
        }
        PermissionMaster permission = new PermissionMaster();
        permission.setPermissionId(idGenerator.generateUnique("PERM", permissionRepository::existsById));
        permission.setPermissionName(request.getPermissionName().trim());
        permission.setPermissionCode(request.getPermissionCode().trim());
        return mapToDTO(permissionRepository.save(permission));
    }

    public PermissionDTO update(String id, PermissionUpdateRequest request) {
        PermissionMaster permission = requireById(id);
        if (request.getPermissionName() != null) {
            permission.setPermissionName(request.getPermissionName().trim());
        }
        if (request.getPermissionCode() != null) {
            String code = request.getPermissionCode().trim();
            if (!code.equals(permission.getPermissionCode()) && permissionRepository.existsByPermissionCode(code)) {
                throw new DuplicateResourceException(
                        "A permission with code '" + code + "' already exists.");
            }
            permission.setPermissionCode(code);
        }
        return mapToDTO(permissionRepository.save(permission));
    }

    /**
     * Deletes the permission. Role-permission mappings that reference it
     * cascade at the database level per the supplied schema.
     */
    public void delete(String id) {
        permissionRepository.delete(requireById(id));
    }

    public PermissionDTO mapToDTO(PermissionMaster permission) {
        PermissionDTO dto = new PermissionDTO();
        dto.setPermissionId(permission.getPermissionId());
        dto.setPermissionName(permission.getPermissionName());
        dto.setPermissionCode(permission.getPermissionCode());
        dto.setCreatedDatetime(permission.getCreatedDatetime());
        dto.setUpdatedDatetime(permission.getUpdatedDatetime());
        return dto;
    }

    private PermissionMaster requireById(String id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));
    }
}
