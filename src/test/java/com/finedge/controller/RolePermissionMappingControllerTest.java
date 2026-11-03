package com.finedge.controller;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RolePermissionMappingDTO;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.service.RolePermissionMappingService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RolePermissionMappingController.class)
class RolePermissionMappingControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

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

    private RolePermissionMappingDTO dto(String id) {
        RolePermissionMappingDTO dto = new RolePermissionMappingDTO();
        dto.setMappingId(id);
        dto.setOrgId("ORG-1");
        dto.setRoleId("ROLE-1");
        dto.setModuleId("MOD-1");
        dto.setPermissionId("PERM-1");
        return dto;
    }

    @Test
    @DisplayName("GET /api/etfdb/role-mappings returns a page")
    void list_ReturnsPage() throws Exception {
        PageResponse<RolePermissionMappingDTO> page =
                new PageResponse<>(List.of(dto("MAP-1")), 0, 10, 1, 1, true, true);
        when(mappingService.list(isNull(), isNull(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/role-mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].mappingId").value("MAP-1"));
    }

    @Test
    @DisplayName("POST /api/etfdb/role-mappings returns 201")
    void create_Returns201() throws Exception {
        when(mappingService.create(any())).thenReturn(dto("MAP-NEW"));

        mvc.perform(post("/api/etfdb/role-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"ORG-1\",\"roleId\":\"ROLE-1\","
                                + "\"moduleId\":\"MOD-1\",\"permissionId\":\"PERM-1\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mappingId").value("MAP-NEW"));
    }

    @Test
    @DisplayName("POST duplicate mapping returns 409")
    void create_DuplicateReturns409() throws Exception {
        when(mappingService.create(any()))
                .thenThrow(new DuplicateResourceException(
                        "This organization/role/module/permission assignment already exists."));

        mvc.perform(post("/api/etfdb/role-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"ORG-1\",\"roleId\":\"ROLE-1\","
                                + "\"moduleId\":\"MOD-1\",\"permissionId\":\"PERM-1\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/etfdb/role-mappings returns 400 for invalid body")
    void create_InvalidReturns400() throws Exception {
        mvc.perform(post("/api/etfdb/role-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET unknown mapping returns 404")
    void getById_UnknownReturns404() throws Exception {
        when(mappingService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("Role-permission mapping not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/role-mappings/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/etfdb/role-mappings/{id} returns 204")
    void delete_Returns204() throws Exception {
        mvc.perform(delete("/api/etfdb/role-mappings/MAP-1")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }
}
