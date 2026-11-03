package com.finedge.controller;

import com.finedge.dto.LoanActivityStatusCreateRequest;
import com.finedge.dto.LoanActivityStatusDTO;
import com.finedge.dto.PageResponse;
import com.finedge.service.LoanActivityService;
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
@RequestMapping("/api/etfdb/loan-activities")
public class LoanActivityController {

    private final LoanActivityService activityService;

    public LoanActivityController(LoanActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<LoanActivityStatusDTO>> list(
            @RequestParam(required = false) String loanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(activityService.list(loanId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanActivityStatusDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(activityService.getById(id));
    }

    @PostMapping
    public ResponseEntity<LoanActivityStatusDTO> create(
            @Valid @RequestBody LoanActivityStatusCreateRequest request) {
        LoanActivityStatusDTO created = activityService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
