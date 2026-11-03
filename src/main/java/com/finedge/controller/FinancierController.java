package com.finedge.controller;

import com.finedge.dto.FinancierProfile;
import com.finedge.dto.FinancierProfileCreateRequest;
import com.finedge.dto.FinancierProfileUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.service.FinancierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * M2 Part C controller for the financier directory.
 *
 * <p>GET list/detail are public (permit-all in {@link
 * com.finedge.config.SecurityConfig}) so the directory can be browsed
 * before authentication. Write operations require the
 * {@code ROLE_FINANCIER_ADMIN} role via {@link PreAuthorize}.
 */
@RestController
@RequestMapping("/api/etfdb/financiers")
public class FinancierController {

    private final FinancierService financierService;

    public FinancierController(FinancierService financierService) {
        this.financierService = financierService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<FinancierProfile>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(financierService.list(status, search, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinancierProfile> getById(@PathVariable String id) {
        return ResponseEntity.ok(financierService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_FINANCIER_ADMIN')")
    public ResponseEntity<FinancierProfile> create(
            @Valid @RequestBody FinancierProfileCreateRequest request) {
        FinancierProfile created = financierService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_FINANCIER_ADMIN')")
    public ResponseEntity<FinancierProfile> update(
            @PathVariable String id,
            @Valid @RequestBody FinancierProfileUpdateRequest request) {
        return ResponseEntity.ok(financierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_FINANCIER_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        financierService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
