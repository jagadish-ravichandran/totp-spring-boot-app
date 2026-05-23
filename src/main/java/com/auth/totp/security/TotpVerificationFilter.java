package com.auth.totp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Blocks access to protected pages if the user is authenticated (password OK)
 * but has NOT yet completed TOTP verification.
 */
public class TotpVerificationFilter extends OncePerRequestFilter {

    // Paths that are always accessible — don't block these
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/verify-2fa", "/login", "/logout", "/register",
            "/css", "/js", "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();

        boolean isAuthenticated = auth != null && auth.isAuthenticated()
                && !auth.getAuthorities().isEmpty()
                && !"anonymousUser".equals(auth.getPrincipal());

        if (isAuthenticated && !isAllowedPath(path)) {
            Object verified = request.getSession().getAttribute(
                    TotpAuthSuccessHandler.TOTP_VERIFIED_SESSION_KEY);

            // If session flag is explicitly false → TOTP pending, redirect
            if (Boolean.FALSE.equals(verified)) {
                response.sendRedirect("/verify-2fa");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedPath(String path) {
        return ALLOWED_PATHS.stream().anyMatch(path::startsWith);
    }
}