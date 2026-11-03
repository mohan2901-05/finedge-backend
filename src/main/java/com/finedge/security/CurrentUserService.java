package com.finedge.security;

import com.finedge.entity.UserInfo;
import com.finedge.repository.UserInfoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Groundwork for role-based authorization: exposes the identity of the
 * currently authenticated user from the security context.
 *
 * <p>This does <em>not</em> perform resource-level authorisation; borrower- and
 * financier-specific data isolation is deferred to later phases. It merely
 * makes the authenticated user's id and roles available to services and
 * controllers.
 */
@Service
public class CurrentUserService {

    private final UserInfoRepository userRepository;

    public CurrentUserService(UserInfoRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return Optional.of(((org.springframework.security.core.userdetails.UserDetails) principal).getUsername());
        }
        return Optional.empty();
    }

    public Set<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return java.util.Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /**
     * Loads the full {@link UserInfo} for the current user, or empty if none is
     * authenticated. Used when a service needs the user's organisation/role.
     */
    public Optional<UserInfo> getCurrentUser() {
        return getCurrentUserId().flatMap(userRepository::findById);
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
