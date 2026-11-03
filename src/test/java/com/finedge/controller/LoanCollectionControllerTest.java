package com.finedge.controller;

import com.finedge.dto.LoanCollectionDTO;
import com.finedge.dto.PageResponse;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.service.CollectionService;
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

@WebMvcTest(LoanCollectionController.class)
class LoanCollectionControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

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

    private LoanCollectionDTO dto(String id) {
        LoanCollectionDTO dto = new LoanCollectionDTO();
        dto.setCollectionId(id);
        dto.setOrgId("ORG-1");
        dto.setLoanId("LOAN-1");
        dto.setUserId("USR-1");
        dto.setCollectionAmount(new BigDecimal("1000.00"));
        dto.setStatus("SUCCESS");
        return dto;
    }

    @Test
    @DisplayName("GET /api/etfdb/collections returns a page")
    void list_ReturnsPage() throws Exception {
        PageResponse<LoanCollectionDTO> page =
                new PageResponse<>(List.of(dto("COLL-1")), 0, 10, 1, 1, true, true);
        when(collectionService.list(isNull(), isNull(), isNull(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].collectionId").value("COLL-1"));
    }

    @Test
    @DisplayName("GET unknown collection returns 404")
    void getById_UnknownReturns404() throws Exception {
        when(collectionService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("Collection not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/collections/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/etfdb/collections returns 201")
    void create_Returns201() throws Exception {
        when(collectionService.create(any())).thenReturn(dto("COLL-NEW"));

        mvc.perform(post("/api/etfdb/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"ORG-1\",\"loanId\":\"LOAN-1\","
                                + "\"userId\":\"USR-1\",\"collectionAmount\":1000.00}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.collectionId").value("COLL-NEW"));
    }

    @Test
    @DisplayName("POST /api/etfdb/collections returns 400 for invalid body")
    void create_InvalidReturns400() throws Exception {
        mvc.perform(post("/api/etfdb/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/etfdb/collections/{id} returns 204")
    void delete_Returns204() throws Exception {
        mvc.perform(delete("/api/etfdb/collections/COLL-1")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }
}
