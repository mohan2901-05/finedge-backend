package com.finedge.controller;

import com.finedge.dto.AuditLogDTO;
import com.finedge.dto.PageResponse;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.service.AuditService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
class AuditLogControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private AuditService auditService;

    private MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").with(
                        SecurityMockMvcRequestPostProcessors.user("USR-TEST").roles("USER")))
                .build();
    }

    private AuditLogDTO dto(String id) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setAuditId(id);
        dto.setOrgId("ORG-1");
        dto.setUserId("USR-1");
        dto.setUserName("Test User");
        dto.setAction("LOGIN");
        return dto;
    }

    @Test
    @DisplayName("GET /api/etfdb/audit-logs returns a page")
    void list_ReturnsPage() throws Exception {
        PageResponse<AuditLogDTO> page =
                new PageResponse<>(List.of(dto("AUD-1")), 0, 10, 1, 1, true, true);
        when(auditService.list(isNull(), isNull(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("LOGIN"));
    }

    @Test
    @DisplayName("GET unknown audit entry returns 404")
    void getById_UnknownReturns404() throws Exception {
        when(auditService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("Audit entry not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/audit-logs/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/etfdb/audit-logs returns 201")
    void create_Returns201() throws Exception {
        when(auditService.create(any())).thenReturn(dto("AUD-NEW"));

        mvc.perform(post("/api/etfdb/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"ORG-1\",\"userId\":\"USR-1\","
                                + "\"userName\":\"Test User\",\"action\":\"LOGIN\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auditId").value("AUD-NEW"));
    }

    @Test
    @DisplayName("POST /api/etfdb/audit-logs returns 400 for invalid body")
    void create_InvalidReturns400() throws Exception {
        mvc.perform(post("/api/etfdb/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }
}
