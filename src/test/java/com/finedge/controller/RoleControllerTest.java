package com.finedge.controller;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RoleDTO;
import com.finedge.dto.RolePermissionMappingDTO;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.service.RolePermissionMappingService;
import com.finedge.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleController.class)
class RoleControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private RoleService roleService;

    @MockBean
    private RolePermissionMappingService mappingService;

    private MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").with(
                        SecurityMockMvcRequestPostProcessors.user("USR-TEST").roles("USER")))
                .build();
    }

    private RoleDTO dto(String id) {
        RoleDTO dto = new RoleDTO();
        dto.setRoleId(id);
        dto.setOrgId("ORG-1");
        dto.setRoleName("Financier");
        dto.setStatus("ACTIVE");
        return dto;
    }

    @Test
    @DisplayName("GET /api/etfdb/roles returns a page")
    void list_ReturnsPage() throws Exception {
        PageResponse<RoleDTO> page =
                new PageResponse<>(List.of(dto("ROLE-1")), 0, 10, 1, 1, true, true);
        when(roleService.list(isNull(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].roleId").value("ROLE-1"));
    }

    @Test
    @DisplayName("GET /api/etfdb/roles/{id}/permissions returns mappings")
    void permissions_ReturnsMappings() throws Exception {
        when(roleService.getById("ROLE-1")).thenReturn(dto("ROLE-1"));
        RolePermissionMappingDTO mapping = new RolePermissionMappingDTO();
        mapping.setMappingId("MAP-1");
        mapping.setRoleId("ROLE-1");
        mapping.setPermissionCode("APPROVE");
        PageResponse<RolePermissionMappingDTO> page =
                new PageResponse<>(List.of(mapping), 0, 10, 1, 1, true, true);
        when(mappingService.list(isNull(), anyString(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/roles/ROLE-1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].mappingId").value("MAP-1"));
    }

    @Test
    @DisplayName("GET unknown role returns 404")
    void getById_UnknownReturns404() throws Exception {
        when(roleService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("Role not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/roles/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/etfdb/roles returns 201")
    void create_Returns201() throws Exception {
        when(roleService.create(any())).thenReturn(dto("ROLE-NEW"));

        mvc.perform(post("/api/etfdb/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"ORG-1\",\"roleName\":\"Financier\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleId").value("ROLE-NEW"));
    }

    @Test
    @DisplayName("POST /api/etfdb/roles returns 400 for invalid body")
    void create_InvalidReturns400() throws Exception {
        mvc.perform(post("/api/etfdb/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"\",\"roleName\":\"\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/etfdb/roles/{id} returns 204")
    void delete_Returns204() throws Exception {
        mvc.perform(delete("/api/etfdb/roles/ROLE-1")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }
}
