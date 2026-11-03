package com.finedge.controller;

import com.finedge.dto.LoanActivityStatusDTO;
import com.finedge.dto.PageResponse;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.service.LoanActivityService;
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

@WebMvcTest(LoanActivityController.class)
class LoanActivityControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private LoanActivityService activityService;

    private MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").with(
                        SecurityMockMvcRequestPostProcessors.user("USR-TEST").roles("USER")))
                .build();
    }

    private LoanActivityStatusDTO dto(String id) {
        LoanActivityStatusDTO dto = new LoanActivityStatusDTO();
        dto.setActivityId(id);
        dto.setUserId("USR-1");
        dto.setLoanId("LOAN-1");
        dto.setActivityCode("DID");
        dto.setStatusCode("D");
        return dto;
    }

    @Test
    @DisplayName("GET /api/etfdb/loan-activities returns a page")
    void list_ReturnsPage() throws Exception {
        PageResponse<LoanActivityStatusDTO> page =
                new PageResponse<>(List.of(dto("LAS-1")), 0, 10, 1, 1, true, true);
        when(activityService.list(isNull(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/loan-activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].activityCode").value("DID"));
    }

    @Test
    @DisplayName("GET unknown activity returns 404")
    void getById_UnknownReturns404() throws Exception {
        when(activityService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("Loan activity not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/loan-activities/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/etfdb/loan-activities returns 201")
    void create_Returns201() throws Exception {
        when(activityService.create(any())).thenReturn(dto("LAS-NEW"));

        mvc.perform(post("/api/etfdb/loan-activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"USR-1\",\"loanId\":\"LOAN-1\","
                                + "\"activityCode\":\"DID\",\"statusCode\":\"D\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityId").value("LAS-NEW"));
    }

    @Test
    @DisplayName("POST /api/etfdb/loan-activities returns 400 for invalid body")
    void create_InvalidReturns400() throws Exception {
        mvc.perform(post("/api/etfdb/loan-activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityCode\":\"\",\"statusCode\":\"TOOLONG\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }
}
