package com.finedge.controller;

import com.finedge.config.SecurityConfig;
import com.finedge.dto.DisbursementDTO;
import com.finedge.dto.OutstandingDTO;
import com.finedge.dto.PageResponse;
import com.finedge.dto.RepaymentDTO;
import com.finedge.exception.BusinessRuleException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.security.JwtService;
import com.finedge.service.LoanDisbursementService;
import com.finedge.service.LoanRepaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanDisbursementController.class)
@Import(SecurityConfig.class)
class LoanDisbursementControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private LoanDisbursementService disbursementService;

    @MockBean
    private LoanRepaymentService repaymentService;

    @MockBean
    private JwtService jwtService;

    private MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").with(
                        SecurityMockMvcRequestPostProcessors.user("USR-BOR").roles("USER")))
                .build();
    }

    private DisbursementDTO disbursementDto() {
        DisbursementDTO dto = new DisbursementDTO();
        dto.setDisbursementId("DISB-1");
        dto.setApplicationId(42);
        dto.setDisbursedAmount(new BigDecimal("50000.00"));
        dto.setLoanAmount(new BigDecimal("50000.00"));
        dto.setDisbursedAt(LocalDateTime.now());
        dto.setDisbursedByUserId("USR-FIN");
        dto.setCreatedDatetime(LocalDateTime.now());
        return dto;
    }

    private RepaymentDTO repaymentDto() {
        RepaymentDTO dto = new RepaymentDTO();
        dto.setRepaymentId("REPAY-1");
        dto.setApplicationId(42);
        dto.setRepaymentAmount(new BigDecimal("5000.00"));
        dto.setPaidAt(LocalDateTime.now());
        dto.setPaidByUserId("USR-BOR");
        dto.setCreatedDatetime(LocalDateTime.now());
        return dto;
    }

    @Test
    @DisplayName("POST /loan-applications/{id}/disburse returns 201 for an approved loan (financier admin)")
    void disburse_Success_Returns201() throws Exception {
        when(disbursementService.recordDisbursement(eq(42), any())).thenReturn(disbursementDto());

        mvc.perform(post("/api/etfdb/loan-applications/42/disburse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disbursedAmount\":50000.00}")
                        .with(SecurityMockMvcRequestPostProcessors.user("USR-FIN").roles("FINANCIER_ADMIN"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.disbursementId").value("DISB-1"))
                .andExpect(jsonPath("$.disbursedAmount").value(50000.00));
    }

    @Test
    @DisplayName("POST /loan-applications/{id}/disburse requires ROLE_FINANCIER_ADMIN")
    void disburse_RequiresFinancierAdmin_Returns403() throws Exception {
        mvc.perform(post("/api/etfdb/loan-applications/42/disburse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disbursedAmount\":50000.00}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /loan-applications/{id}/disburse rejects invalid amount")
    void disburse_InvalidAmount_Returns400() throws Exception {
        mvc.perform(post("/api/etfdb/loan-applications/42/disburse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disbursedAmount\":-100.00}")
                        .with(SecurityMockMvcRequestPostProcessors.user("USR-FIN").roles("FINANCIER_ADMIN"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /loan-applications/{id}/disburse business rule violation returns 400")
    void disburse_BusinessRule_Returns400() throws Exception {
        when(disbursementService.recordDisbursement(eq(42), any()))
                .thenThrow(new BusinessRuleException("only 'Approved' loans may be disbursed"));
        mvc.perform(post("/api/etfdb/loan-applications/42/disburse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disbursedAmount\":50000.00}")
                        .with(SecurityMockMvcRequestPostProcessors.user("USR-FIN").roles("FINANCIER_ADMIN"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /loan-applications/{id}/disburse missing application returns 404")
    void disburse_NotFound_Returns404() throws Exception {
        when(disbursementService.recordDisbursement(eq(42), any()))
                .thenThrow(new ResourceNotFoundException("Loan application not found with id: 42"));
        mvc.perform(post("/api/etfdb/loan-applications/42/disburse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disbursedAmount\":50000.00}")
                        .with(SecurityMockMvcRequestPostProcessors.user("USR-FIN").roles("FINANCIER_ADMIN"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /loan-applications/{id}/disbursement returns 200")
    void getDisbursement_Success_Returns200() throws Exception {
        when(disbursementService.getDisbursement(42)).thenReturn(disbursementDto());
        mvc.perform(get("/api/etfdb/loan-applications/42/disbursement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disbursementId").value("DISB-1"));
    }

    @Test
    @DisplayName("GET /loan-applications/{id}/disbursement missing returns 404")
    void getDisbursement_NotFound_Returns404() throws Exception {
        when(disbursementService.getDisbursement(42))
                .thenThrow(new ResourceNotFoundException("Disbursement not found for loan application id: 42"));
        mvc.perform(get("/api/etfdb/loan-applications/42/disbursement"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /loan-applications/{id}/repayments returns 201")
    void repay_Success_Returns201() throws Exception {
        when(repaymentService.recordRepayment(eq(42), any())).thenReturn(repaymentDto());
        mvc.perform(post("/api/etfdb/loan-applications/42/repayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repaymentAmount\":5000.00}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repaymentId").value("REPAY-1"));
    }

    @Test
    @DisplayName("POST /loan-applications/{id}/repayments rejects invalid amount")
    void repay_InvalidAmount_Returns400() throws Exception {
        mvc.perform(post("/api/etfdb/loan-applications/42/repayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repaymentAmount\":0}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /loan-applications/{id}/repayments business rule violation returns 400")
    void repay_BusinessRule_Returns400() throws Exception {
        when(repaymentService.recordRepayment(eq(42), any()))
                .thenThrow(new BusinessRuleException("Repayment amount exceeds the outstanding balance"));
        mvc.perform(post("/api/etfdb/loan-applications/42/repayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repaymentAmount\":6000.00}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /loan-applications/{id}/repayments missing application returns 404")
    void repay_NotFound_Returns404() throws Exception {
        when(repaymentService.recordRepayment(eq(42), any()))
                .thenThrow(new ResourceNotFoundException("Loan application not found with id: 42"));
        mvc.perform(post("/api/etfdb/loan-applications/42/repayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repaymentAmount\":5000.00}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /loan-applications/{id}/repayments returns a page")
    void listRepayments_ReturnsPage() throws Exception {
        PageResponse<RepaymentDTO> page = new PageResponse<>(List.of(repaymentDto()), 0, 10, 1, 1, true, true);
        when(repaymentService.listRepayments(eq(42), eq(0), eq(10))).thenReturn(page);
        mvc.perform(get("/api/etfdb/loan-applications/42/repayments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].repaymentId").value("REPAY-1"));
    }

    @Test
    @DisplayName("GET /loan-applications/{id}/outstanding returns balance")
    void outstanding_ReturnsBalance() throws Exception {
        OutstandingDTO dto = new OutstandingDTO();
        dto.setApplicationId(42);
        dto.setDisbursed(true);
        dto.setDisbursedAmount(new BigDecimal("50000.00"));
        dto.setTotalRepaid(new BigDecimal("20000.00"));
        dto.setOutstandingAmount(new BigDecimal("30000.00"));
        dto.setFullyRepaid(false);
        when(disbursementService.getOutstanding(42)).thenReturn(dto);
        mvc.perform(get("/api/etfdb/loan-applications/42/outstanding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outstandingAmount").value(30000.00))
                .andExpect(jsonPath("$.fullyRepaid").value(false));
    }
}
