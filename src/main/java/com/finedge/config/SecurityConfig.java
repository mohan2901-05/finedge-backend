package com.finedge.config;

import com.finedge.exception.ErrorResponse;
import com.finedge.security.JwtAuthenticationFilter;
import com.finedge.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Arrays;
import java.util.List;

/**
 * Phase 1 security configuration.
 *
 * <p>Stateless JWT authentication: no HTTP sessions, no CSRF (token-based auth
 * is not CSRF-vulnerable), no form login. All endpoints require authentication
 * except the public auth endpoints ({@code /api/etfdb/auth/**}).
 *
 * <p>Secure default: every non-public endpoint requires an authenticated
 * session. Role-specific restrictions (BORROWER / FINANCIER / ADMIN) are not
 * yet enforced per-endpoint because the role requirements are undefined in
 * Phase 1; {@link org.springframework.security.access.prepost.PreAuthorize} is
 * enabled so later phases can add them without changing this chain.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String[] PUBLIC_AUTH_PATHS = {"/api/etfdb/auth/**"};

    /**
     * Public read-only financier-directory paths. Browsable before
     * authentication so the directory can be used without logging in.
     * Write operations (POST/PUT/DELETE) remain protected by {@code
     * @PreAuthorize} on the controller.
     */
    private static final String[] PUBLIC_READ_PATHS = {
            "/api/etfdb/financiers",
            "/api/etfdb/financiers/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // Stateless: no sessions are created or used by Spring Security.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Token-based authentication is not CSRF-vulnerable; CSRF would
                // only break the Angular SPA and the JWT flow.
                .csrf(csrf -> csrf.disable())
                // CORS is handled for the configured Angular dev origin(s).
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_AUTH_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_READ_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper().writeValueAsString(
                                    new ErrorResponse(
                                            HttpServletResponse.SC_UNAUTHORIZED,
                                            "Unauthorized",
                                            "Authentication required",
                                            request.getRequestURI())));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper().writeValueAsString(
                                    new ErrorResponse(
                                            HttpServletResponse.SC_FORBIDDEN,
                                            "Forbidden",
                                            "Insufficient privileges",
                                            request.getRequestURI())));
                        })
                );
        return http.build();
    }

    /**
     * CORS configuration mirrored from {@link WebConfig} so Spring Security's
     * CORS filter (which runs ahead of authorisation) can answer preflight
     * requests for the Angular development origin without falling through to
     * an authenticated endpoint.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:4200", "http://127.0.0.1:4200"));
        configuration.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name(), HttpMethod.PATCH.name()));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

