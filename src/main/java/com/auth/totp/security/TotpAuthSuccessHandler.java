package com.auth.totp.security;

import com.auth.totp.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TotpAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    // @Lazy breaks the circular dependency:
    // SecurityConfig → TotpAuthSuccessHandler → AuthService → SecurityConfig
    public TotpAuthSuccessHandler(@Lazy AuthService authService) {
        this.authService = authService;
    }

    // Session key — set to true only after TOTP is verified
    public static final String TOTP_VERIFIED_SESSION_KEY = "TOTP_VERIFIED";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();

        if (authService.is2FaEnabled(username)) {
            // Mark TOTP as NOT yet verified for this session
            request.getSession().setAttribute(TOTP_VERIFIED_SESSION_KEY, false);
            response.sendRedirect("/verify-2fa");
        } else {
            // No 2FA — go straight to dashboard
            request.getSession().setAttribute(TOTP_VERIFIED_SESSION_KEY, true);
            response.sendRedirect("/dashboard");
        }
    }
}