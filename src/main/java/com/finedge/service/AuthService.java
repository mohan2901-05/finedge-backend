package com.finedge.service;

import com.finedge.dto.AuthRegisterRequest;
import com.finedge.dto.AuthLoginRequest;
import com.finedge.dto.AuthResponse;
import com.finedge.dto.UserCreateRequest;
import com.finedge.dto.UserDTO;
import com.finedge.entity.Organization;
import com.finedge.entity.RoleMaster;
import com.finedge.entity.UserInfo;
import com.finedge.exception.AuthException;
import com.finedge.exception.BusinessRuleException;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.RoleMasterRepository;
import com.finedge.repository.UserInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finedge.security.JwtService;
import java.time.Instant;

/**
 * Authentication service: self-service registration and credential login.
 *
 * <p>Registration assigns only the configured default borrower role and
 * organisation; a client can never self-assign administrator or financier
 * privileges. Passwords are hashed via {@link PasswordEncoder} before
 * persistence (delegated to {@link UserService#create}).
 *
 * <p>Login verifies the submitted password against the stored BCrypt hash and
 * returns a short-lived JWT. Failure is always reported with the same generic
 * message so an attacker cannot distinguish "unknown email" from "wrong
 * password".
 */
@Service
public class AuthService {

    private final UserService userService;
    private final UserInfoRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleMasterRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expirationMs;

    private final String defaultOrgId;
    private final String defaultBorrowerRoleId;

    public AuthService(UserService userService,
                       UserInfoRepository userRepository,
                       OrganizationRepository organizationRepository,
                       RoleMasterRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${finedge.auth.default-org-id:}") String defaultOrgId,
                       @Value("${finedge.auth.default-borrower-role-id:}") String defaultBorrowerRoleId,
                       @Value("${finedge.jwt.expiration-ms:900000}") long expirationMs) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.defaultOrgId = defaultOrgId;
        this.defaultBorrowerRoleId = defaultBorrowerRoleId;
        this.expirationMs = expirationMs;
    }

    /**
     * Registers a new borrower user. Server-assigned organisation and role only.
     */
    @Transactional
    public UserDTO register(AuthRegisterRequest request) {
        requireDefaultOrgConfigured();
        requireDefaultBorrowerRoleConfigured();

        Organization org = organizationRepository.findById(defaultOrgId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Default organisation '" + defaultOrgId + "' is not configured."));
        RoleMaster role = roleRepository.findById(defaultBorrowerRoleId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Default borrower role '" + defaultBorrowerRoleId + "' is not configured."));

        UserCreateRequest createRequest = new UserCreateRequest();
        createRequest.setOrgId(defaultOrgId);
        createRequest.setRoleId(defaultBorrowerRoleId);
        createRequest.setUserName(request.getUserName());
        createRequest.setPassword(request.getPassword());
        createRequest.setUserEmail(request.getUserEmail());
        createRequest.setDob(request.getDob());
        createRequest.setPanNo(request.getPanNo());
        // Status defaults to ACTIVE inside UserService.create.
        return userService.create(createRequest);
    }

    /**
     * Authenticates email/password and issues an access token.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(AuthLoginRequest request) {
        String email = request.getUserEmail() != null ? request.getUserEmail().trim() : "";
        UserInfo user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        long expiresInSeconds = expirationMs / 1000;
        Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds);
        UserDTO userDto = userService.mapToDTO(user);
        return new AuthResponse(token, "Bearer", expiresInSeconds, expiresAt, userDto);
    }

    private void requireDefaultOrgConfigured() {
        if (defaultOrgId == null || defaultOrgId.isBlank()) {
            throw new BusinessRuleException(
                    "Registration is not configured: default organisation is missing.");
        }
    }

    private void requireDefaultBorrowerRoleConfigured() {
        if (defaultBorrowerRoleId == null || defaultBorrowerRoleId.isBlank()) {
            throw new BusinessRuleException(
                    "Registration is not configured: default borrower role is missing.");
        }
    }
}
