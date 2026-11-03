package com.finedge.controller;

import com.finedge.dto.PageResponse;
import com.finedge.dto.UserDTO;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.service.UserService;
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

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private UserService userService;

    private MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").with(
                        SecurityMockMvcRequestPostProcessors.user("USR-TEST").roles("USER")))
                .build();
    }

    private UserDTO dto(String id) {
        UserDTO dto = new UserDTO();
        dto.setUserId(id);
        dto.setOrgId("ORG-1");
        dto.setRoleId("ROLE-1");
        dto.setUserName("Test User");
        dto.setUserEmail("user@example.invalid");
        dto.setStatus("ACTIVE");
        return dto;
    }

    @Test
    @DisplayName("GET /api/etfdb/users returns a page without password or PAN")
    void list_ReturnsPageWithoutSecrets() throws Exception {
        PageResponse<UserDTO> page =
                new PageResponse<>(List.of(dto("USR-1")), 0, 10, 1, 1, true, true);
        when(userService.list(isNull(), isNull(), anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/api/etfdb/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("USR-1"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].panNo").doesNotExist());
    }

    @Test
    @DisplayName("GET unknown user returns 404")
    void getById_UnknownReturns404() throws Exception {
        when(userService.getById("NOPE"))
                .thenThrow(new ResourceNotFoundException("User not found with id: NOPE"));

        mvc.perform(get("/api/etfdb/users/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/etfdb/users returns 201")
    void create_Returns201() throws Exception {
        when(userService.create(any())).thenReturn(dto("USR-NEW"));

        mvc.perform(post("/api/etfdb/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"ORG-1\",\"roleId\":\"ROLE-1\","
                                + "\"userName\":\"Test User\",\"password\":\"secret-value\","
                                + "\"userEmail\":\"user@example.invalid\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("USR-NEW"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/etfdb/users returns 400 for invalid body")
    void create_InvalidReturns400() throws Exception {
        mvc.perform(post("/api/etfdb/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"\",\"userEmail\":\"bad\"}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/etfdb/users/{id} returns 204")
    void delete_Returns204() throws Exception {
        mvc.perform(delete("/api/etfdb/users/USR-1")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }
}
