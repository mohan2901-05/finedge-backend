package com.finedge.controller;

import com.finedge.dto.OrganizationCreateRequest;
import com.finedge.dto.OrganizationDTO;
import com.finedge.dto.OrganizationUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.service.OrganizationService;
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
@RequestMapping("/api/etfdb/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrganizationDTO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(organizationService.list(status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @PostMapping
    public ResponseEntity<OrganizationDTO> create(@Valid @RequestBody OrganizationCreateRequest request) {
        OrganizationDTO created = organizationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationDTO> update(
            @PathVariable String id,
            @Valid @RequestBody OrganizationUpdateRequest request) {
        return ResponseEntity.ok(organizationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
