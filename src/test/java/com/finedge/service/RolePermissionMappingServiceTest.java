package com.finedge.service;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RolePermissionMappingCreateRequest;
import com.finedge.dto.RolePermissionMappingDTO;
import com.finedge.entity.ModuleMaster;
import com.finedge.entity.Organization;
import com.finedge.entity.PermissionMaster;
import com.finedge.entity.RoleMaster;
import com.finedge.entity.RolePermissionMapping;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.ModuleMasterRepository;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.PermissionMasterRepository;
import com.finedge.repository.RoleMasterRepository;
import com.finedge.repository.RolePermissionMappingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolePermissionMappingServiceTest {

    @Mock
    private RolePermissionMappingRepository mappingRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RoleMasterRepository roleRepository;

    @Mock
    private ModuleMasterRepository moduleRepository;

    @Mock
    private PermissionMasterRepository permissionRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @InjectMocks
    private RolePermissionMappingService mappingService;

    private RolePermissionMappingCreateRequest createRequest() {
        RolePermissionMappingCreateRequest request = new RolePermissionMappingCreateRequest();
        request.setOrgId("ORG-1");
        request.setRoleId("ROLE-1");
        request.setModuleId("MOD-1");
        request.setPermissionId("PERM-1");
        return request;
    }

    private void stubParents() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-1");
        ModuleMaster module = new ModuleMaster();
        module.setModuleId("MOD-1");
        PermissionMaster permission = new PermissionMaster();
        permission.setPermissionId("PERM-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(roleRepository.findById("ROLE-1")).thenReturn(Optional.of(role));
        when(moduleRepository.findById("MOD-1")).thenReturn(Optional.of(module));
        when(permissionRepository.findById("PERM-1")).thenReturn(Optional.of(permission));
    }

    @Test
    @DisplayName("Create assigns a generated ID")
    void create_AssignsId() {
        stubParents();
        when(mappingRepository.existsByOrganization_OrgIdAndRole_RoleIdAndModule_ModuleIdAndPermission_PermissionId(
                "ORG-1", "ROLE-1", "MOD-1", "PERM-1")).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("MAP-ABC123");
        when(mappingRepository.save(any(RolePermissionMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RolePermissionMappingDTO created = mappingService.create(createRequest());

        assertEquals("MAP-ABC123", created.getMappingId());
    }

    @Test
    @DisplayName("Create of a duplicate assignment fails")
    void create_DuplicateFails() {
        stubParents();
        when(mappingRepository.existsByOrganization_OrgIdAndRole_RoleIdAndModule_ModuleIdAndPermission_PermissionId(
                "ORG-1", "ROLE-1", "MOD-1", "PERM-1")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> mappingService.create(createRequest()));
    }

    @Test
    @DisplayName("Create with an unknown organization fails with 404")
    void create_UnknownOrgFails() {
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> mappingService.create(createRequest()));
        verify(mappingRepository, never()).save(any(RolePermissionMapping.class));
    }

    @Test
    @DisplayName("Create with an unknown role fails with 404")
    void create_UnknownRoleFails() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(roleRepository.findById("ROLE-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> mappingService.create(createRequest()));
        verify(mappingRepository, never()).save(any(RolePermissionMapping.class));
    }

    @Test
    @DisplayName("Create with an unknown module fails with 404")
    void create_UnknownModuleFails() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(roleRepository.findById("ROLE-1")).thenReturn(Optional.of(role));
        when(moduleRepository.findById("MOD-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> mappingService.create(createRequest()));
        verify(mappingRepository, never()).save(any(RolePermissionMapping.class));
    }

    @Test
    @DisplayName("Create with an unknown permission fails with 404")
    void create_UnknownPermissionFails() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-1");
        ModuleMaster module = new ModuleMaster();
        module.setModuleId("MOD-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(roleRepository.findById("ROLE-1")).thenReturn(Optional.of(role));
        when(moduleRepository.findById("MOD-1")).thenReturn(Optional.of(module));
        when(permissionRepository.findById("PERM-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> mappingService.create(createRequest()));
        verify(mappingRepository, never()).save(any(RolePermissionMapping.class));
    }

    @Test
    @DisplayName("Create propagates repository exceptions")
    void create_RepositoryThrows() {
        stubParents();
        when(mappingRepository.existsByOrganization_OrgIdAndRole_RoleIdAndModule_ModuleIdAndPermission_PermissionId(
                "ORG-1", "ROLE-1", "MOD-1", "PERM-1")).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("MAP-ABC123");
        when(mappingRepository.save(any(RolePermissionMapping.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> mappingService.create(createRequest()));
    }

    @Test
    @DisplayName("Get by ID returns denormalized module/permission codes")
    void getById_ReturnsDto() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-1");
        ModuleMaster module = new ModuleMaster();
        module.setModuleId("MOD-1");
        module.setModuleCode("LOANS");
        PermissionMaster permission = new PermissionMaster();
        permission.setPermissionId("PERM-1");
        permission.setPermissionCode("VIEW");
        RolePermissionMapping mapping = new RolePermissionMapping();
        mapping.setMappingId("MAP-1");
        mapping.setOrganization(org);
        mapping.setRole(role);
        mapping.setModule(module);
        mapping.setPermission(permission);
        when(mappingRepository.findById("MAP-1")).thenReturn(Optional.of(mapping));

        RolePermissionMappingDTO dto = mappingService.getById("MAP-1");

        assertEquals("MAP-1", dto.getMappingId());
        assertEquals("LOANS", dto.getModuleCode());
        assertEquals("VIEW", dto.getPermissionCode());
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404")
    void getById_UnknownFails() {
        when(mappingRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> mappingService.getById("NOPE"));
    }

    @Test
    @DisplayName("List without filters queries all mappings")
    void list_AllReturnsPage() {
        RolePermissionMapping mapping = new RolePermissionMapping();
        mapping.setMappingId("MAP-1");
        when(mappingRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mapping)));

        PageResponse<RolePermissionMappingDTO> page = mappingService.list(null, null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(mappingRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List with a role filter queries by role")
    void list_ByRoleId() {
        RolePermissionMapping mapping = new RolePermissionMapping();
        mapping.setMappingId("MAP-1");
        when(mappingRepository.findByRole_RoleId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mapping)));

        PageResponse<RolePermissionMappingDTO> page = mappingService.list(null, "ROLE-1", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(mappingRepository).findByRole_RoleId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with an org filter queries by organization")
    void list_ByOrgId() {
        RolePermissionMapping mapping = new RolePermissionMapping();
        mapping.setMappingId("MAP-1");
        when(mappingRepository.findByOrganization_OrgId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mapping)));

        PageResponse<RolePermissionMappingDTO> page = mappingService.list("ORG-1", null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(mappingRepository).findByOrganization_OrgId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List prefers the role filter when both filters are supplied")
    void list_RoleTakesPriorityOverOrg() {
        RolePermissionMapping mapping = new RolePermissionMapping();
        mapping.setMappingId("MAP-1");
        when(mappingRepository.findByRole_RoleId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mapping)));

        PageResponse<RolePermissionMappingDTO> page = mappingService.list("ORG-1", "ROLE-1", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(mappingRepository).findByRole_RoleId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(mappingRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<RolePermissionMappingDTO> page = mappingService.list(null, null, 0, 10);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Delete revokes the mapping")
    void delete_Removes() {
        RolePermissionMapping mapping = new RolePermissionMapping();
        mapping.setMappingId("MAP-1");
        when(mappingRepository.findById("MAP-1")).thenReturn(Optional.of(mapping));

        mappingService.delete("MAP-1");

        verify(mappingRepository).delete(mapping);
    }

    @Test
    @DisplayName("Delete with an unknown ID fails with 404")
    void delete_UnknownFails() {
        when(mappingRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> mappingService.delete("NOPE"));
    }
}
