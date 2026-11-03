package com.finedge.controller;

import com.finedge.dto.LoanActivityStatusDTO;
import com.finedge.dto.LoanCollectionDTO;
import com.finedge.dto.LoanInfoCreateRequest;
import com.finedge.dto.LoanInfoDTO;
import com.finedge.dto.LoanInfoUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.dto.RepaymentDetailDTO;
import com.finedge.service.CollectionService;
import com.finedge.service.LoanActivityService;
import com.finedge.service.LoanInfoService;
import com.finedge.service.RepaymentService;
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
@RequestMapping("/api/etfdb/loans")
public class LoanInfoController {

    private final LoanInfoService loanService;
    private final LoanActivityService activityService;
    private final RepaymentService repaymentService;
    private final CollectionService collectionService;

    public LoanInfoController(LoanInfoService loanService,
                              LoanActivityService activityService,
                              RepaymentService repaymentService,
                              CollectionService collectionService) {
        this.loanService = loanService;
        this.activityService = activityService;
        this.repaymentService = repaymentService;
        this.collectionService = collectionService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<LoanInfoDTO>> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(loanService.list(orgId, userId, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanInfoDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(loanService.getById(id));
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<PageResponse<LoanActivityStatusDTO>> activities(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        loanService.getById(id);
        return ResponseEntity.ok(activityService.list(id, page, size));
    }

    @GetMapping("/{id}/repayments")
    public ResponseEntity<PageResponse<RepaymentDetailDTO>> repayments(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        loanService.getById(id);
        return ResponseEntity.ok(repaymentService.list(id, page, size));
    }

    @GetMapping("/{id}/collections")
    public ResponseEntity<PageResponse<LoanCollectionDTO>> collections(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        loanService.getById(id);
        return ResponseEntity.ok(collectionService.list(id, null, null, page, size));
    }

    @PostMapping
    public ResponseEntity<LoanInfoDTO> create(@Valid @RequestBody LoanInfoCreateRequest request) {
        LoanInfoDTO created = loanService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanInfoDTO> update(
            @PathVariable String id,
            @Valid @RequestBody LoanInfoUpdateRequest request) {
        return ResponseEntity.ok(loanService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        loanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
