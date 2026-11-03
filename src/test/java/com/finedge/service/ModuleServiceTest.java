package com.finedge.service;

import com.finedge.dto.ModuleCreateRequest;
import com.finedge.dto.ModuleDTO;
import com.finedge.dto.ModuleUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.ModuleMaster;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.ModuleMasterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    @Mock
    private ModuleMasterRepository moduleRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @InjectMocks
    private ModuleService moduleService;

    private ModuleCreateRequest createRequest() {
        ModuleCreateRequest request = new ModuleCreateRequest();
        request.setModuleName("Loans ");
        request.setModuleCode(" LOANS ");
        return request;
    }

    private ModuleMaster existingModule() {
        ModuleMaster module = new ModuleMaster();
        module.setModuleId("MOD-1");
        module.setModuleName("Loans");
        module.setModuleCode("LOANS");
        return module;
    }

    @Test
    @DisplayName("Create assigns a generated ID and trims name/code")
    void create_AssignsId() {
        when(moduleRepository.existsByModuleCode(" LOANS ")).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("MOD-ABC123");
        when(moduleRepository.save(any(ModuleMaster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ModuleDTO created = moduleService.create(createRequest());

        assertEquals("MOD-ABC123", created.getModuleId());
        assertEquals("Loans", created.getModuleName());
        assertEquals("LOANS", created.getModuleCode());
        ArgumentCaptor<ModuleMaster> saved = ArgumentCaptor.forClass(ModuleMaster.class);
        verify(moduleRepository).save(saved.capture());
        assertEquals("MOD-ABC123", saved.getValue().getModuleId());
        verify(idGenerator).generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any());
    }

    @Test
    @DisplayName("Create with a duplicate code fails")
    void create_DuplicateCodeFails() {
        when(moduleRepository.existsByModuleCode(" LOANS ")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> moduleService.create(createRequest()));
    }

    @Test
    @DisplayName("Create propagates repository exceptions")
    void create_RepositoryThrows() {
        when(moduleRepository.existsByModuleCode(" LOANS ")).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("MOD-ABC123");
        when(moduleRepository.save(any(ModuleMaster.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> moduleService.create(createRequest()));
    }

    @Test
    @DisplayName("Get by ID returns the DTO")
    void getById_ReturnsDto() {
        when(moduleRepository.findById("MOD-1")).thenReturn(Optional.of(existingModule()));

        ModuleDTO dto = moduleService.getById("MOD-1");

        assertEquals("MOD-1", dto.getModuleId());
        assertEquals("LOANS", dto.getModuleCode());
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404 semantics")
    void getById_UnknownFails() {
        when(moduleRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> moduleService.getById("NOPE"));
    }

    @Test
    @DisplayName("List returns a mapped page")
    void list_ReturnsPage() {
        ModuleMaster module = existingModule();
        when(moduleRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(module)));

        PageResponse<ModuleDTO> page = moduleService.list(0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("MOD-1", page.getContent().get(0).getModuleId());
        verify(moduleRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(moduleRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<ModuleDTO> page = moduleService.list(0, 10);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Update changes name and code")
    void update_Success() {
        ModuleUpdateRequest request = new ModuleUpdateRequest();
        request.setModuleName(" Collections ");
        request.setModuleCode("COLL");
        when(moduleRepository.findById("MOD-1")).thenReturn(Optional.of(existingModule()));
        when(moduleRepository.existsByModuleCode("COLL")).thenReturn(false);
        when(moduleRepository.save(any(ModuleMaster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ModuleDTO updated = moduleService.update("MOD-1", request);

        assertEquals("Collections", updated.getModuleName());
        assertEquals("COLL", updated.getModuleCode());
    }

    @Test
    @DisplayName("Update keeping the same code does not trigger duplicate check failure")
    void update_SameCodeAllows() {
        ModuleUpdateRequest request = new ModuleUpdateRequest();
        request.setModuleCode("LOANS");
        when(moduleRepository.findById("MOD-1")).thenReturn(Optional.of(existingModule()));
        // Even if the code were taken elsewhere, the unchanged code must not
        // trigger the duplicate guard: the lenient stub only takes effect if
        // the implementation wrongly consults the repository.
        lenient().when(moduleRepository.existsByModuleCode("LOANS")).thenReturn(true);
        when(moduleRepository.save(any(ModuleMaster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ModuleDTO updated = moduleService.update("MOD-1", request);

        assertEquals("LOANS", updated.getModuleCode());
        verify(moduleRepository).save(any(ModuleMaster.class));
        verify(moduleRepository, never()).existsByModuleCode(anyString());
    }

    @Test
    @DisplayName("Update to a taken code fails")
    void update_DuplicateCodeFails() {
        ModuleUpdateRequest request = new ModuleUpdateRequest();
        request.setModuleCode("TAKEN");
        when(moduleRepository.findById("MOD-1")).thenReturn(Optional.of(existingModule()));
        when(moduleRepository.existsByModuleCode("TAKEN")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> moduleService.update("MOD-1", request));
    }

    @Test
    @DisplayName("Update with an unknown ID fails with 404")
    void update_UnknownFails() {
        ModuleUpdateRequest request = new ModuleUpdateRequest();
        request.setModuleName("New");
        when(moduleRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> moduleService.update("NOPE", request));
    }

    @Test
    @DisplayName("Delete removes the module")
    void delete_Removes() {
        ModuleMaster module = existingModule();
        when(moduleRepository.findById("MOD-1")).thenReturn(Optional.of(module));

        moduleService.delete("MOD-1");

        verify(moduleRepository).delete(module);
    }

    @Test
    @DisplayName("Delete with an unknown ID fails with 404")
    void delete_UnknownFails() {
        when(moduleRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> moduleService.delete("NOPE"));
    }
}
