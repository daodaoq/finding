package com.finding.interceptor;

import com.finding.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Static helper to get the current authenticated user ID from Spring Security's
 * SecurityContext. No longer an interceptor — auth is handled by
 * JwtAuthenticationFilter.
 */
public final class JwtInterceptor {

    private JwtInterceptor() {}

    /**
     * Returns the current user ID from SecurityContext, or null if not authenticated.
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal p) {
            return p.getId();
        }
        if (principal instanceof Long id) {
            return id;
        }
        // Fallback: try parsing the principal name
        try {
            return Long.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
