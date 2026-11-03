package com.finedge.controller;

import com.finedge.dto.ModuleDTO;
import com.finedge.dto.PageResponse;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.service.ModuleService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModuleController.class)
class ModuleControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private ModuleService moduleService;

    private MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").with(
                        SecurityMockMvcRequestPostProcessors.user("USR-TEST").roles("USER")))
                .build();
    }

    private ModuleDTO dto(String id) {
        ModuleDTO dto = new ModuleDTO();
        dto.setModuleId(id);
        dto.setModuleName("Loans");
        dto.setModuleCode("LOANS");
        return dto;
    }

    @Test
    @DisplayName("GET /api/etfdb/modules returns a page")
    void list_ReturnsPage() throws Exception {
        PageResponse<ModuleDTO> page =
                new PageResponse<>(List.of(dto("MOD-1")), 0, 10, 1, 1, true, true);
        when(moduleService.list(anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].moduleCode").value("LOANS"));
    }

    @Test
    @DisplayName("GET unknown module returns 404")
    void getById_UnknownReturns404() throws Exception {
        when(moduleService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("Module not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/modules/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/etfdb/modules returns 201")
    void create_Returns201() throws Exception {
        when(moduleService.create(any())).thenReturn(dto("MOD-NEW"));

        mvc.perform(post("/api/etfdb/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moduleName\":\"Loans\",\"moduleCode\":\"LOANS\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleId").value("MOD-NEW"));
    }

    @Test
    @DisplayName("POST /api/etfdb/modules returns 400 for invalid body")
    void create_InvalidReturns400() throws Exception {
        mvc.perform(post("/api/etfdb/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moduleName\":\"\",\"moduleCode\":\"\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/etfdb/modules/{id} returns 204")
    void delete_Returns204() throws Exception {
        mvc.perform(delete("/api/etfdb/modules/MOD-1")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }
}
