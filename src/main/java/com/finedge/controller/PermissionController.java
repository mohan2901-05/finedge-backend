package com.finedge.controller;

import com.finedge.dto.PageResponse;
import com.finedge.dto.PermissionCreateRequest;
import com.finedge.dto.PermissionDTO;
import com.finedge.dto.PermissionUpdateRequest;
import com.finedge.service.PermissionService;
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
@RequestMapping("/api/etfdb/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<PermissionDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(permissionService.list(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(permissionService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PermissionDTO> create(@Valid @RequestBody PermissionCreateRequest request) {
        PermissionDTO created = permissionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionDTO> update(
            @PathVariable String id,
            @Valid @RequestBody PermissionUpdateRequest request) {
        return ResponseEntity.ok(permissionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
