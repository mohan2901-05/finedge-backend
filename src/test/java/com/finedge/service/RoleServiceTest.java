package com.finedge.service;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RoleCreateRequest;
import com.finedge.dto.RoleDTO;
import com.finedge.dto.RoleUpdateRequest;
import com.finedge.entity.Organization;
import com.finedge.entity.RoleMaster;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.RoleMasterRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleMasterRepository roleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @InjectMocks
    private RoleService roleService;

    private RoleCreateRequest createRequest() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setOrgId("ORG-1");
        request.setRoleName(" Collector ");
        request.setRoleDescription("Collects dues");
        return request;
    }

    private Organization existingOrg() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        org.setOrgName("Test Org");
        return org;
    }

    private RoleMaster existingRole() {
        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-1");
        role.setOrganization(existingOrg());
        role.setRoleName("Collector");
        role.setRoleDescription("Collects dues");
        role.setStatus("ACTIVE");
        return role;
    }

    @Test
    @DisplayName("Create assigns a generated ID with ACTIVE default and trims the name")
    void create_AssignsId() {
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(existingOrg()));
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("ROLE-ABC123");
        when(roleRepository.save(any(RoleMaster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoleDTO created = roleService.create(createRequest());

        assertEquals("ROLE-ABC123", created.getRoleId());
        assertEquals("Collector", created.getRoleName());
        assertEquals("ACTIVE", created.getStatus());
        assertEquals("ORG-1", created.getOrgId());
        ArgumentCaptor<RoleMaster> saved = ArgumentCaptor.forClass(RoleMaster.class);
        verify(roleRepository).save(saved.capture());
        assertEquals("ROLE-ABC123", saved.getValue().getRoleId());
    }

    @Test
    @DisplayName("Create keeps an explicit status")
    void create_KeepsExplicitStatus() {
        RoleCreateRequest request = createRequest();
        request.setStatus("INACTIVE");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(existingOrg()));
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("ROLE-ABC123");
        when(roleRepository.save(any(RoleMaster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoleDTO created = roleService.create(request);

        assertEquals("INACTIVE", created.getStatus());
    }

    @Test
    @DisplayName("Create with an unknown organization fails with 404")
    void create_UnknownOrgFails() {
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.create(createRequest()));
    }

    @Test
    @DisplayName("Create propagates repository exceptions")
    void create_RepositoryThrows() {
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(existingOrg()));
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("ROLE-ABC123");
        when(roleRepository.save(any(RoleMaster.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> roleService.create(createRequest()));
    }

    @Test
    @DisplayName("Get by ID returns the DTO")
    void getById_ReturnsDto() {
        when(roleRepository.findById("ROLE-1")).thenReturn(Optional.of(existingRole()));

        RoleDTO dto = roleService.getById("ROLE-1");

        assertEquals("ROLE-1", dto.getRoleId());
        assertEquals("ORG-1", dto.getOrgId());
        assertEquals("Collector", dto.getRoleName());
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404")
    void getById_UnknownFails() {
        when(roleRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.getById("NOPE"));
    }

    @Test
    @DisplayName("List without a filter queries all roles")
    void list_AllReturnsPage() {
        RoleMaster role = existingRole();
        when(roleRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(role)));

        PageResponse<RoleDTO> page = roleService.list(null, 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("ROLE-1", page.getContent().get(0).getRoleId());
        verify(roleRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List with an org filter queries by organization")
    void list_ByOrgId() {
        RoleMaster role = existingRole();
        when(roleRepository.findByOrganization_OrgId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(role)));

        PageResponse<RoleDTO> page = roleService.list("ORG-1", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(roleRepository).findByOrganization_OrgId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(roleRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<RoleDTO> page = roleService.list(null, 0, 10);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Update changes mutable fields")
    void update_Success() {
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRoleName(" Senior Collector ");
        request.setRoleDescription("Senior");
        request.setStatus("INACTIVE");
        when(roleRepository.findById("ROLE-1")).thenReturn(Optional.of(existingRole()));
        when(roleRepository.save(any(RoleMaster.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoleDTO updated = roleService.update("ROLE-1", request);

        assertEquals("Senior Collector", updated.getRoleName());
        assertEquals("Senior", updated.getRoleDescription());
        assertEquals("INACTIVE", updated.getStatus());
    }

    @Test
    @DisplayName("Update with an unknown ID fails with 404")
    void update_UnknownFails() {
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRoleName("New");
        when(roleRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.update("NOPE", request));
    }

    @Test
    @DisplayName("Delete removes the role")
    void delete_Removes() {
        RoleMaster role = existingRole();
        when(roleRepository.findById("ROLE-1")).thenReturn(Optional.of(role));

        roleService.delete("ROLE-1");

        verify(roleRepository).delete(role);
    }

    @Test
    @DisplayName("Delete with an unknown ID fails with 404")
    void delete_UnknownFails() {
        when(roleRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.delete("NOPE"));
    }
}
