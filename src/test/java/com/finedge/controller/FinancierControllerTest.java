package com.finedge.controller;

import com.finedge.dto.FinancierProfile;
import com.finedge.dto.PageResponse;
import com.finedge.exception.BusinessRuleException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.service.FinancierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for the financier directory.
 *
 * Uses {@link SpringBootTest} (full context) so the Phase 1
 * {@link com.finedge.config.SecurityConfig} is active: GET endpoints are
 * public, write endpoints require {@code ROLE_FINANCIER_ADMIN}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class FinancierControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private FinancierService financierService;

    private FinancierProfile dto(String id) {
        FinancierProfile dto = new FinancierProfile();
        dto.setFinancierId(id);
        dto.setInstitutionName("Alpha Capital");
        dto.setDescription("Term lending specialist");
        dto.setIndicativeAnnualRate(new BigDecimal("12.50"));
        dto.setMinLoanAmount(new BigDecimal("50000.00"));
        dto.setMaxLoanAmount(new BigDecimal("5000000.00"));
        dto.setAvailableTenureMonths(24);
        dto.setStatus("ACTIVE");
        return dto;
    }

    private String createBody() {
        return "{\"institutionName\":\"Alpha Capital\","
                + "\"description\":\"Term lending specialist\","
                + "\"indicativeAnnualRate\":12.50,"
                + "\"minLoanAmount\":50000.00,"
                + "\"maxLoanAmount\":5000000.00,"
                + "\"availableTenureMonths\":24,"
                + "\"status\":\"ACTIVE\"}";
    }

    @Test
    @DisplayName("GET /api/etfdb/financiers returns a page (public, no auth)")
    void list_ReturnsPage_Public() throws Exception {
        PageResponse<FinancierProfile> page =
                new PageResponse<>(List.of(dto("FIN-1")), 0, 10, 1, 1, true, true);
        when(financierService.list(isNull(), isNull(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/financiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].institutionName").value("Alpha Capital"));
    }

    @Test
    @DisplayName("GET /api/etfdb/financiers/search returns filtered page (public, no auth)")
    void list_WithSearch_Public() throws Exception {
        PageResponse<FinancierProfile> page =
                new PageResponse<>(List.of(dto("FIN-1")), 0, 10, 1, 1, true, true);
        when(financierService.list(isNull(), anyString(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/financiers?search=Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].institutionName").value("Alpha Capital"));
    }

    @Test
    @DisplayName("GET /api/etfdb/financiers/{id} returns the financier (public, no auth)")
    void getById_ReturnsDto_Public() throws Exception {
        when(financierService.getById("FIN-1")).thenReturn(dto("FIN-1"));

        mvc.perform(get("/api/etfdb/financiers/FIN-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financierId").value("FIN-1"))
                .andExpect(jsonPath("$.institutionName").value("Alpha Capital"));
    }

    @Test
    @DisplayName("GET unknown financier returns 404 (public, no auth)")
    void getById_UnknownReturns404_Public() throws Exception {
        when(financierService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("Financier not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/financiers/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/etfdb/financiers returns 201 (authorized admin)")
    void create_Returns201_Authorized() throws Exception {
        when(financierService.create(any())).thenReturn(dto("FIN-NEW"));

        mvc.perform(post("/api/etfdb/financiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody())
                        .with(SecurityMockMvcRequestPostProcessors.user("USR-ADMIN").roles("FINANCIER_ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.financierId").value("FIN-NEW"));
    }

    @Test
    @DisplayName("POST /api/etfdb/financiers returns 401 without authentication")
    void create_Returns401_Unauthenticated() throws Exception {
        mvc.perform(post("/api/etfdb/financiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/etfdb/financiers returns 403 with non-admin authenticated role")
    void create_Returns403_WrongRole() throws Exception {
        mvc.perform(post("/api/etfdb/financiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody())
                        .with(SecurityMockMvcRequestPostProcessors.user("USR-BORROWER").roles("BORROWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/etfdb/financiers returns 400 for invalid body (authorized)")
    void create_InvalidReturns400_Authorized() throws Exception {
        mvc.perform(post("/api/etfdb/financiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institutionName\":\"\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("USR-ADMIN").roles("FINANCIER_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/etfdb/financiers returns 400 for business rule violation (authorized)")
    void create_BusinessRuleReturns400_Authorized() throws Exception {
        when(financierService.create(any()))
                .thenThrow(new BusinessRuleException("Minimum loan amount must not exceed maximum loan amount."));

        mvc.perform(post("/api/etfdb/financiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody())
                        .with(SecurityMockMvcRequestPostProcessors.user("USR-ADMIN").roles("FINANCIER_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/etfdb/financiers/{id} returns 200 (authorized admin)")
    void update_Returns200_Authorized() throws Exception {
        when(financierService.update(anyString(), any())).thenReturn(dto("FIN-1"));

        mvc.perform(put("/api/etfdb/financiers/FIN-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institutionName\":\"Updated Name\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("USR-ADMIN").roles("FINANCIER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financierId").value("FIN-1"));
    }

    @Test
    @DisplayName("PUT /api/etfdb/financiers/{id} returns 401 without authentication")
    void update_Returns401_Unauthenticated() throws Exception {
        mvc.perform(put("/api/etfdb/financiers/FIN-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institutionName\":\"Updated Name\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/etfdb/financiers/{id} returns 403 with non-admin authenticated role")
    void update_Returns403_WrongRole() throws Exception {
        mvc.perform(put("/api/etfdb/financiers/FIN-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institutionName\":\"Updated Name\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("USR-BORROWER").roles("BORROWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/etfdb/financiers/{id} returns 204 (authorized admin)")
    void deactivate_Returns204_Authorized() throws Exception {
        mvc.perform(delete("/api/etfdb/financiers/FIN-1")
                .with(SecurityMockMvcRequestPostProcessors.user("USR-ADMIN").roles("FINANCIER_ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/etfdb/financiers/{id} returns 401 without authentication")
    void deactivate_Returns401_Unauthenticated() throws Exception {
        mvc.perform(delete("/api/etfdb/financiers/FIN-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/etfdb/financiers/{id} returns 403 with non-admin authenticated role")
    void deactivate_Returns403_WrongRole() throws Exception {
        mvc.perform(delete("/api/etfdb/financiers/FIN-1")
                .with(SecurityMockMvcRequestPostProcessors.user("USR-BORROWER").roles("BORROWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE unknown financier returns 404 (authorized)")
    void deactivate_UnknownReturns404_Authorized() throws Exception {
        doThrow(new ResourceNotFoundException("Financier not found with id: NOPE"))
                .when(financierService).deactivate("NOPE");

        mvc.perform(delete("/api/etfdb/financiers/NOPE")
                .with(SecurityMockMvcRequestPostProcessors.user("USR-ADMIN").roles("FINANCIER_ADMIN")))
                .andExpect(status().isNotFound());
    }
}
