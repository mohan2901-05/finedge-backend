package com.finedge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Extracts a Bearer JWT from the {@code Authorization} header and, when valid,
 * populates the Spring Security context with the authenticated user.
 *
 * <p>Stateless: the principal and authorities are reconstructed from the
 * (cryptographically validated) token claims, so no database lookup is required
 * per request. No token (or an invalid/expired token) leaves the context cleared
 * so downstream authorisation rejects the request with 401.
 *
 * <p>Registered as a {@link org.springframework.context.annotation.Bean} in
 * {@link com.finedge.config.SecurityConfig} rather than via {@code @Component}
 * so that web-layer test slices ({@code @WebMvcTest}) which do not scan
 * {@code @Service} beans are not forced to initialise it.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token) && jwtService.validateToken(token)) {
                String userId = jwtService.extractUserId(token);
                String roleName = extractRoleName(token);
                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(userId)
                        .password("")
                        .authorities(authorities(roleName))
                        .accountExpired(false)
                        .accountLocked(false)
                        .credentialsExpired(false)
                        .disabled(false)
                        .build();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Invalid / expired token: clear context so the request is rejected.
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private String extractRoleName(String token) {
        try {
            return (String) jwtService.extractClaims(token).get("roleName");
        } catch (Exception e) {
            return null;
        }
    }

    private Collection<? extends org.springframework.security.core.GrantedAuthority>
            authorities(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return List.of();
        }
        return List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        ROLE_PREFIX + roleName));
    }
}
