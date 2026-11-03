package com.finedge.controller;

import com.finedge.dto.LoanApplicationCreateRequest;
import com.finedge.dto.LoanApplicationDTO;
import com.finedge.dto.LoanApplicationUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.service.LoanApplicationService;
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

@RestController
@RequestMapping("/api/etfdb/loan-applications")
public class LoanApplicationController {

    private final LoanApplicationService applicationService;

    public LoanApplicationController(LoanApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<LoanApplicationDTO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(applicationService.list(status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(applicationService.getById(id));
    }

    @GetMapping("/ref/{ref}")
    public ResponseEntity<LoanApplicationDTO> getByRef(@PathVariable String ref) {
        return ResponseEntity.ok(applicationService.getByRef(ref));
    }

    @PostMapping
    public ResponseEntity<LoanApplicationDTO> create(@Valid @RequestBody LoanApplicationCreateRequest request) {
        LoanApplicationDTO created = applicationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanApplicationDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody LoanApplicationUpdateRequest request) {
        return ResponseEntity.ok(applicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        applicationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Financier-scoped list: returns the current financier's own applications.
     * Requires {@code ROLE_FINANCIER_ADMIN} and an associated financier
     * institution on the user's record.
     */
    @GetMapping("/financier")
    @PreAuthorize("hasAuthority('ROLE_FINANCIER_ADMIN')")
    public ResponseEntity<PageResponse<LoanApplicationDTO>> listForFinancier(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(applicationService.listForFinancier(status, page, size));
    }

    /**
     * Financier-scoped get: returns a single application owned by the current
     * financier's institution. Uses 404 (not 403) to avoid leaking existence.
     */
    @GetMapping("/financier/{id}")
    @PreAuthorize("hasAuthority('ROLE_FINANCIER_ADMIN')")
    public ResponseEntity<LoanApplicationDTO> getByIdForFinancier(@PathVariable Integer id) {
        return ResponseEntity.ok(applicationService.getByIdForFinancier(id));
    }

    /**
     * Approves a {@code Pending Review} application owned by the current
     * financier's institution. Requires {@code ROLE_FINANCIER_ADMIN}.
     */
    @PutMapping("/financier/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_FINANCIER_ADMIN')")
    public ResponseEntity<LoanApplicationDTO> approve(@PathVariable Integer id) {
        return ResponseEntity.ok(applicationService.approve(id));
    }

    /**
     * Rejects a {@code Pending Review} application owned by the current
     * financier's institution. Requires {@code ROLE_FINANCIER_ADMIN}.
     */
    @PutMapping("/financier/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_FINANCIER_ADMIN')")
    public ResponseEntity<LoanApplicationDTO> reject(@PathVariable Integer id) {
        return ResponseEntity.ok(applicationService.reject(id));
    }
}
