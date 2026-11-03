package com.finedge.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finedge.dto.AuthLoginRequest;
import com.finedge.dto.AuthRegisterRequest;
import com.finedge.dto.AuthResponse;
import com.finedge.dto.UserDTO;
import com.finedge.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Auth endpoint tests. Registration and login are permit-all in the real
 * security chain, but the {@code @WebMvcTest} slice uses Spring Security's
 * default chain, so the requests are authenticated here with a test user and
 * a CSRF token (matching the stateless JWT setup where CSRF is disabled in
 * production).
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private MockMvc mvc;

    @BeforeEach
    void init() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").with(
                        SecurityMockMvcRequestPostProcessors.user("USR-TEST").roles("USER")))
                .build();
    }

    @Test
    void registerReturnsCreated() throws Exception {
        UserDTO dto = new UserDTO();
        dto.setUserId("USR-1");
        dto.setUserEmail("new@example.invalid");
        dto.setRoleId("ROLE-BORROWER-TEST");
        when(authService.register(any())).thenReturn(dto);

        mvc.perform(post("/api/etfdb/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("USR-1"))
                .andExpect(jsonPath("$.roleId").value("ROLE-BORROWER-TEST"));
    }

    @Test
    void loginReturnsToken() throws Exception {
        AuthResponse response = new AuthResponse();
        response.setAccessToken("eyJvalid.token");
        response.setTokenType("Bearer");
        response.setExpiresIn(900L);
        when(authService.login(any())).thenReturn(response);

        mvc.perform(post("/api/etfdb/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("eyJvalid.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    private AuthRegisterRequest regRequest() {
        AuthRegisterRequest r = new AuthRegisterRequest();
        r.setUserName("New User");
        r.setPassword("Secret123!");
        r.setUserEmail("new@example.invalid");
        return r;
    }

    private AuthLoginRequest loginRequest() {
        AuthLoginRequest r = new AuthLoginRequest();
        r.setUserEmail("user@example.invalid");
        r.setPassword("Secret123!");
        return r;
    }
}
