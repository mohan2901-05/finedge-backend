package com.finedge.controller;

import com.finedge.dto.AuthLoginRequest;
import com.finedge.dto.AuthRegisterRequest;
import com.finedge.dto.AuthResponse;
import com.finedge.dto.UserDTO;
import com.finedge.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints (registration and login).
 *
 * <p>Both are permit-all in {@link com.finedge.config.SecurityConfig}. All
 * other endpoints require a valid JWT.
 */
@RestController
@RequestMapping("/api/etfdb/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody AuthRegisterRequest request) {
        UserDTO created = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
