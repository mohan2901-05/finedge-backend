package com.finedge.controller;

import com.finedge.dto.AuditLogCreateRequest;
import com.finedge.dto.AuditLogDTO;
import com.finedge.dto.PageResponse;
import com.finedge.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/etfdb/audit-logs")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<AuditLogDTO>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(auditService.list(orgId, userId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLogDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(auditService.getById(id));
    }

    @PostMapping
    public ResponseEntity<AuditLogDTO> create(@Valid @RequestBody AuditLogCreateRequest request) {
        AuditLogDTO created = auditService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
