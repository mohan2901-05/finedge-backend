package com.finedge.controller;

import com.finedge.dto.OrganizationDTO;
import com.finedge.dto.PageResponse;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.service.OrganizationService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private OrganizationService organizationService;

    private MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").with(
                        SecurityMockMvcRequestPostProcessors.user("USR-TEST").roles("USER")))
                .build();
    }

    private OrganizationDTO dto(String id) {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setOrgId(id);
        dto.setOrgName("Test Org");
        dto.setOrgEmail("test@example.invalid");
        dto.setStatus("ACTIVE");
        return dto;
    }

    @Test
    @DisplayName("GET /api/etfdb/organizations returns a page")
    void list_ReturnsPage() throws Exception {
        PageResponse<OrganizationDTO> page =
                new PageResponse<>(List.of(dto("ORG-1")), 0, 10, 1, 1, true, true);
        when(organizationService.list(isNull(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orgId").value("ORG-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/etfdb/organizations/{id} returns 404 for unknown id")
    void getById_UnknownReturns404() throws Exception {
        when(organizationService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("Organization not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/organizations/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/etfdb/organizations returns 201")
    void create_Returns201() throws Exception {
        when(organizationService.create(any())).thenReturn(dto("ORG-NEW"));

        mvc.perform(post("/api/etfdb/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgName\":\"Test Org\",\"orgEmail\":\"test@example.invalid\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orgId").value("ORG-NEW"));
    }

    @Test
    @DisplayName("POST /api/etfdb/organizations returns 400 for invalid body")
    void create_InvalidReturns400() throws Exception {
        mvc.perform(post("/api/etfdb/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgName\":\"\",\"orgEmail\":\"not-an-email\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/etfdb/organizations/{id} returns 204")
    void delete_Returns204() throws Exception {
        mvc.perform(delete("/api/etfdb/organizations/ORG-1")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE unknown organization returns 404")
    void delete_UnknownReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Organization not found with id: NOPE"))
                .when(organizationService).delete(anyString());

        mvc.perform(delete("/api/etfdb/organizations/NOPE")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }
}
