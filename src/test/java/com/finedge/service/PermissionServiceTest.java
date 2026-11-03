package com.finedge.service;

import com.finedge.dto.PageResponse;
import com.finedge.dto.PermissionCreateRequest;
import com.finedge.dto.PermissionDTO;
import com.finedge.dto.PermissionUpdateRequest;
import com.finedge.entity.PermissionMaster;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.PermissionMasterRepository;
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
class PermissionServiceTest {

    @Mock
    private PermissionMasterRepository permissionRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @InjectMocks
    private PermissionService permissionService;

    private PermissionCreateRequest createRequest() {
        PermissionCreateRequest request = new PermissionCreateRequest();
        request.setPermissionName("View ");
        request.setPermissionCode(" VIEW ");
        return request;
    }

    private PermissionMaster existingPermission() {
        PermissionMaster permission = new PermissionMaster();
        permission.setPermissionId("PERM-1");
        permission.setPermissionName("View");
        permission.setPermissionCode("VIEW");
        return permission;
    }

    @Test
    @DisplayName("Create assigns a generated ID and trims name/code")
    void create_AssignsId() {
        when(permissionRepository.existsByPermissionCode(" VIEW ")).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("PERM-ABC123");
        when(permissionRepository.save(any(PermissionMaster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PermissionDTO created = permissionService.create(createRequest());

        assertEquals("PERM-ABC123", created.getPermissionId());
        assertEquals("View", created.getPermissionName());
        assertEquals("VIEW", created.getPermissionCode());
        ArgumentCaptor<PermissionMaster> saved = ArgumentCaptor.forClass(PermissionMaster.class);
        verify(permissionRepository).save(saved.capture());
        assertEquals("PERM-ABC123", saved.getValue().getPermissionId());
    }

    @Test
    @DisplayName("Create with a duplicate code fails")
    void create_DuplicateCodeFails() {
        when(permissionRepository.existsByPermissionCode(" VIEW ")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> permissionService.create(createRequest()));
    }

    @Test
    @DisplayName("Create propagates repository exceptions")
    void create_RepositoryThrows() {
        when(permissionRepository.existsByPermissionCode(" VIEW ")).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("PERM-ABC123");
        when(permissionRepository.save(any(PermissionMaster.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> permissionService.create(createRequest()));
    }

    @Test
    @DisplayName("Get by ID returns the DTO")
    void getById_ReturnsDto() {
        when(permissionRepository.findById("PERM-1")).thenReturn(Optional.of(existingPermission()));

        PermissionDTO dto = permissionService.getById("PERM-1");

        assertEquals("PERM-1", dto.getPermissionId());
        assertEquals("VIEW", dto.getPermissionCode());
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404 semantics")
    void getById_UnknownFails() {
        when(permissionRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> permissionService.getById("NOPE"));
    }

    @Test
    @DisplayName("List returns a mapped page")
    void list_ReturnsPage() {
        PermissionMaster permission = existingPermission();
        when(permissionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(permission)));

        PageResponse<PermissionDTO> page = permissionService.list(0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("PERM-1", page.getContent().get(0).getPermissionId());
        verify(permissionRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(permissionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<PermissionDTO> page = permissionService.list(0, 10);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Update changes name and code")
    void update_Success() {
        PermissionUpdateRequest request = new PermissionUpdateRequest();
        request.setPermissionName(" Edit ");
        request.setPermissionCode("EDIT");
        when(permissionRepository.findById("PERM-1")).thenReturn(Optional.of(existingPermission()));
        when(permissionRepository.existsByPermissionCode("EDIT")).thenReturn(false);
        when(permissionRepository.save(any(PermissionMaster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PermissionDTO updated = permissionService.update("PERM-1", request);

        assertEquals("Edit", updated.getPermissionName());
        assertEquals("EDIT", updated.getPermissionCode());
    }

    @Test
    @DisplayName("Update keeping the same code does not trigger duplicate check failure")
    void update_SameCodeAllows() {
        PermissionUpdateRequest request = new PermissionUpdateRequest();
        request.setPermissionCode("VIEW");
        when(permissionRepository.findById("PERM-1")).thenReturn(Optional.of(existingPermission()));
        // Even if the code were taken elsewhere, the unchanged code must not
        // trigger the duplicate guard: the lenient stub only takes effect if
        // the implementation wrongly consults the repository.
        lenient().when(permissionRepository.existsByPermissionCode("VIEW")).thenReturn(true);
        when(permissionRepository.save(any(PermissionMaster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PermissionDTO updated = permissionService.update("PERM-1", request);

        assertEquals("VIEW", updated.getPermissionCode());
        verify(permissionRepository).save(any(PermissionMaster.class));
        verify(permissionRepository, never()).existsByPermissionCode(anyString());
    }

    @Test
    @DisplayName("Update to a taken code fails")
    void update_DuplicateCodeFails() {
        PermissionUpdateRequest request = new PermissionUpdateRequest();
        request.setPermissionCode("TAKEN");
        when(permissionRepository.findById("PERM-1")).thenReturn(Optional.of(existingPermission()));
        when(permissionRepository.existsByPermissionCode("TAKEN")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> permissionService.update("PERM-1", request));
    }

    @Test
    @DisplayName("Update with an unknown ID fails with 404")
    void update_UnknownFails() {
        PermissionUpdateRequest request = new PermissionUpdateRequest();
        request.setPermissionName("New");
        when(permissionRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> permissionService.update("NOPE", request));
    }

    @Test
    @DisplayName("Delete removes the permission")
    void delete_Removes() {
        PermissionMaster permission = existingPermission();
        when(permissionRepository.findById("PERM-1")).thenReturn(Optional.of(permission));

        permissionService.delete("PERM-1");

        verify(permissionRepository).delete(permission);
    }

    @Test
    @DisplayName("Delete with an unknown ID fails with 404")
    void delete_UnknownFails() {
        when(permissionRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> permissionService.delete("NOPE"));
    }
}
