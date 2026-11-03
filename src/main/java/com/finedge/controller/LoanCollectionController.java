package com.finedge.controller;

import com.finedge.dto.LoanCollectionCreateRequest;
import com.finedge.dto.LoanCollectionDTO;
import com.finedge.dto.LoanCollectionUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.service.CollectionService;
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
@RequestMapping("/api/etfdb/collections")
public class LoanCollectionController {

    private final CollectionService collectionService;

    public LoanCollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<LoanCollectionDTO>> list(
            @RequestParam(required = false) String loanId,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(collectionService.list(loanId, orgId, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanCollectionDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(collectionService.getById(id));
    }

    @PostMapping
    public ResponseEntity<LoanCollectionDTO> create(@Valid @RequestBody LoanCollectionCreateRequest request) {
        LoanCollectionDTO created = collectionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanCollectionDTO> update(
            @PathVariable String id,
            @Valid @RequestBody LoanCollectionUpdateRequest request) {
        return ResponseEntity.ok(collectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        collectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
