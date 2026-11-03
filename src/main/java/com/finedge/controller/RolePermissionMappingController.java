package com.finedge.controller;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RolePermissionMappingCreateRequest;
import com.finedge.dto.RolePermissionMappingDTO;
import com.finedge.service.RolePermissionMappingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/etfdb/role-mappings")
public class RolePermissionMappingController {

    private final RolePermissionMappingService mappingService;

    public RolePermissionMappingController(RolePermissionMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<RolePermissionMappingDTO>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String roleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(mappingService.list(orgId, roleId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolePermissionMappingDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(mappingService.getById(id));
    }

    @PostMapping
    public ResponseEntity<RolePermissionMappingDTO> create(
            @Valid @RequestBody RolePermissionMappingCreateRequest request) {
        RolePermissionMappingDTO created = mappingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        mappingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
