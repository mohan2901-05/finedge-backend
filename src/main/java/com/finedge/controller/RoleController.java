package com.finedge.controller;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RoleCreateRequest;
import com.finedge.dto.RoleDTO;
import com.finedge.dto.RolePermissionMappingDTO;
import com.finedge.dto.RoleUpdateRequest;
import com.finedge.service.RolePermissionMappingService;
import com.finedge.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/etfdb/roles")
public class RoleController {

    private final RoleService roleService;
    private final RolePermissionMappingService mappingService;

    public RoleController(RoleService roleService,
                          RolePermissionMappingService mappingService) {
        this.roleService = roleService;
        this.mappingService = mappingService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<RoleDTO>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(roleService.list(orgId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    /**
     * Read-only view of the permissions assigned to a role. This reports
     * stored assignments; it does not enforce them on any endpoint.
     */
    @GetMapping("/{id}/permissions")
    public ResponseEntity<PageResponse<RolePermissionMappingDTO>> permissions(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        roleService.getById(id);
        return ResponseEntity.ok(mappingService.list(null, id, page, size));
    }

    @PostMapping
    public ResponseEntity<RoleDTO> create(@Valid @RequestBody RoleCreateRequest request) {
        RoleDTO created = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDTO> update(
            @PathVariable String id,
            @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
