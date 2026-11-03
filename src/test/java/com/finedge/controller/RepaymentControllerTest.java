package com.finedge.controller;

import com.finedge.dto.PageResponse;
import com.finedge.dto.RepaymentDetailDTO;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.service.RepaymentService;
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

import java.math.BigDecimal;
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

@WebMvcTest(RepaymentController.class)
class RepaymentControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private RepaymentService repaymentService;

    private MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").with(
                        SecurityMockMvcRequestPostProcessors.user("USR-TEST").roles("USER")))
                .build();
    }

    private RepaymentDetailDTO dto(String id) {
        RepaymentDetailDTO dto = new RepaymentDetailDTO();
        dto.setRepaymentId(id);
        dto.setLoanId("LOAN-1");
        dto.setInstallmentNumber(1);
        dto.setInstallmentAmount(new BigDecimal("1000.00"));
        return dto;
    }

    @Test
    @DisplayName("GET /api/etfdb/repayments returns a page")
    void list_ReturnsPage() throws Exception {
        PageResponse<RepaymentDetailDTO> page =
                new PageResponse<>(List.of(dto("REP-1")), 0, 10, 1, 1, true, true);
        when(repaymentService.list(isNull(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/repayments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].repaymentId").value("REP-1"));
    }

    @Test
    @DisplayName("GET unknown repayment returns 404")
    void getById_UnknownReturns404() throws Exception {
        when(repaymentService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("Repayment not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/repayments/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/etfdb/repayments returns 201")
    void create_Returns201() throws Exception {
        when(repaymentService.create(any())).thenReturn(dto("REP-NEW"));

        mvc.perform(post("/api/etfdb/repayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanId\":\"LOAN-1\",\"installmentNumber\":1,"
                                + "\"dueDate\":\"2026-02-07\",\"openingPrincipal\":120000.00,"
                                + "\"installmentAmount\":1000.00,\"principal\":900.00,"
                                + "\"interest\":100.00,\"closingPrincipal\":119100.00,"
                                + "\"rate\":12.00}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repaymentId").value("REP-NEW"));
    }

    @Test
    @DisplayName("POST /api/etfdb/repayments returns 400 for invalid body")
    void create_InvalidReturns400() throws Exception {
        mvc.perform(post("/api/etfdb/repayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanId\":\"\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/etfdb/repayments/{id} returns 204")
    void delete_Returns204() throws Exception {
        mvc.perform(delete("/api/etfdb/repayments/REP-1")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }
}
