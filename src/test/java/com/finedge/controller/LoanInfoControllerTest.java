package com.finedge.controller;

import com.finedge.dto.LoanInfoDTO;
import com.finedge.dto.PageResponse;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.service.CollectionService;
import com.finedge.service.LoanActivityService;
import com.finedge.service.LoanInfoService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanInfoController.class)
class LoanInfoControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private LoanInfoService loanService;

    @MockBean
    private LoanActivityService activityService;

    @MockBean
    private RepaymentService repaymentService;

    @MockBean
    private CollectionService collectionService;

    private MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").with(
                        SecurityMockMvcRequestPostProcessors.user("USR-TEST").roles("USER")))
                .build();
    }

    private LoanInfoDTO dto(String id) {
        LoanInfoDTO dto = new LoanInfoDTO();
        dto.setLoanId(id);
        dto.setOrgId("ORG-1");
        dto.setUserId("USR-1");
        dto.setLoanAmount(new BigDecimal("120000.00"));
        dto.setTenureMonths(12);
        dto.setReceived(false);
        dto.setStatus("DISBURSED");
        return dto;
    }

    @Test
    @DisplayName("GET /api/etfdb/loans returns a page")
    void list_ReturnsPage() throws Exception {
        PageResponse<LoanInfoDTO> page =
                new PageResponse<>(List.of(dto("LOAN-1")), 0, 10, 1, 1, true, true);
        when(loanService.list(isNull(), isNull(), isNull(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].loanId").value("LOAN-1"));
    }

    @Test
    @DisplayName("GET /api/etfdb/loans/{id}/activities delegates to the loan scope")
    void activities_Delegates() throws Exception {
        when(loanService.getById("LOAN-1")).thenReturn(dto("LOAN-1"));
        PageResponse<com.finedge.dto.LoanActivityStatusDTO> page =
                new PageResponse<>(List.of(), 0, 10, 0, 0, true, true);
        when(activityService.list(anyString(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/loans/LOAN-1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET unknown loan returns 404")
    void getById_UnknownReturns404() throws Exception {
        when(loanService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("Loan not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/loans/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET activities of unknown loan returns 404")
    void activities_UnknownLoanReturns404() throws Exception {
        when(loanService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("Loan not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/loans/NOPE/activities"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/etfdb/loans returns 201")
    void create_Returns201() throws Exception {
        when(loanService.create(any())).thenReturn(dto("LOAN-NEW"));

        mvc.perform(post("/api/etfdb/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"ORG-1\",\"userId\":\"USR-1\","
                                + "\"loanAmount\":120000.00,\"tenureMonths\":12}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanId").value("LOAN-NEW"));
    }

    @Test
    @DisplayName("POST /api/etfdb/loans returns 400 for invalid body")
    void create_InvalidReturns400() throws Exception {
        mvc.perform(post("/api/etfdb/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/etfdb/loans/{id} returns 204")
    void delete_Returns204() throws Exception {
        mvc.perform(delete("/api/etfdb/loans/LOAN-1")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }
}
