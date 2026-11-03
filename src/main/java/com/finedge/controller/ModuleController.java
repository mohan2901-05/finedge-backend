package com.finedge.controller;

import com.finedge.dto.ModuleCreateRequest;
import com.finedge.dto.ModuleDTO;
import com.finedge.dto.ModuleUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.service.ModuleService;
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
@RequestMapping("/api/etfdb/modules")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ModuleDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(moduleService.list(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModuleDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(moduleService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ModuleDTO> create(@Valid @RequestBody ModuleCreateRequest request) {
        ModuleDTO created = moduleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModuleDTO> update(
            @PathVariable String id,
            @Valid @RequestBody ModuleUpdateRequest request) {
        return ResponseEntity.ok(moduleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        moduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
