package com.finedge.service;

import com.finedge.dto.ModuleCreateRequest;
import com.finedge.dto.ModuleDTO;
import com.finedge.dto.ModuleUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.ModuleMaster;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.ModuleMasterRepository;
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
public class ModuleService {

    private final ModuleMasterRepository moduleRepository;
    private final StringIdGenerator idGenerator;

    public ModuleService(ModuleMasterRepository moduleRepository,
                         StringIdGenerator idGenerator) {
        this.moduleRepository = moduleRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<ModuleDTO> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedDatetime"));
        Page<ModuleMaster> result = moduleRepository.findAll(pageable);
        List<ModuleDTO> dtos = result.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return PageResponse.from(result, dtos);
    }

    @Transactional(readOnly = true)
    public ModuleDTO getById(String id) {
        return mapToDTO(requireById(id));
    }

    public ModuleDTO create(ModuleCreateRequest request) {
        if (moduleRepository.existsByModuleCode(request.getModuleCode())) {
            throw new DuplicateResourceException(
                    "A module with code '" + request.getModuleCode() + "' already exists.");
        }
        ModuleMaster module = new ModuleMaster();
        module.setModuleId(idGenerator.generateUnique("MOD", moduleRepository::existsById));
        module.setModuleName(request.getModuleName().trim());
        module.setModuleCode(request.getModuleCode().trim());
        return mapToDTO(moduleRepository.save(module));
    }

    public ModuleDTO update(String id, ModuleUpdateRequest request) {
        ModuleMaster module = requireById(id);
        if (request.getModuleName() != null) {
            module.setModuleName(request.getModuleName().trim());
        }
        if (request.getModuleCode() != null) {
            String code = request.getModuleCode().trim();
            if (!code.equals(module.getModuleCode()) && moduleRepository.existsByModuleCode(code)) {
                throw new DuplicateResourceException(
                        "A module with code '" + code + "' already exists.");
            }
            module.setModuleCode(code);
        }
        return mapToDTO(moduleRepository.save(module));
    }

    /**
     * Deletes the module. Role-permission mappings that reference it cascade
     * at the database level per the supplied schema.
     */
    public void delete(String id) {
        moduleRepository.delete(requireById(id));
    }

    public ModuleDTO mapToDTO(ModuleMaster module) {
        ModuleDTO dto = new ModuleDTO();
        dto.setModuleId(module.getModuleId());
        dto.setModuleName(module.getModuleName());
        dto.setModuleCode(module.getModuleCode());
        dto.setCreatedDatetime(module.getCreatedDatetime());
        dto.setUpdatedDatetime(module.getUpdatedDatetime());
        return dto;
    }

    private ModuleMaster requireById(String id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + id));
    }
}
