package com.finedge.controller;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RepaymentDetailCreateRequest;
import com.finedge.dto.RepaymentDetailDTO;
import com.finedge.service.RepaymentService;
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
@RequestMapping("/api/etfdb/repayments")
public class RepaymentController {

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<RepaymentDetailDTO>> list(
            @RequestParam(required = false) String loanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(repaymentService.list(loanId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepaymentDetailDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(repaymentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<RepaymentDetailDTO> create(
            @Valid @RequestBody RepaymentDetailCreateRequest request) {
        RepaymentDetailDTO created = repaymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        repaymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
