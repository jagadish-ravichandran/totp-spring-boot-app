package com.auth.totp.controller;

import com.auth.totp.dto.AuthDto.*;
import com.auth.totp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final AuthService authService;

    // Register
    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        ApiResponse response = authService.register(request);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    // Setup 2FA — returns QR code
    @GetMapping("/2fa/setup")
    public ResponseEntity<Setup2FaResponse> setup2Fa(Authentication auth) {
        return ResponseEntity.ok(authService.setup2Fa(auth.getName()));
    }

    // Confirm 2FA enabled
    @PostMapping("/2fa/confirm")
    public ResponseEntity<ApiResponse> confirm2Fa(Authentication auth, @RequestParam int code) {
        ApiResponse response = authService.confirm2Fa(auth.getName(), code);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    // Verify OTP
    @PostMapping("/2fa/verify")
    public ResponseEntity<ApiResponse> verify2Fa(Authentication auth, @RequestParam int code) {
        ApiResponse response = authService.verify2Fa(auth.getName(), code);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(401).body(response);
    }

    // Disable 2FA
    @PostMapping("/2fa/disable")
    public ResponseEntity<ApiResponse> disable2Fa(Authentication auth, @RequestParam int code) {
        ApiResponse response = authService.disable2Fa(auth.getName(), code);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    // Status check
    @GetMapping("/2fa/status")
    public ResponseEntity<?> status2Fa(Authentication auth) {
        boolean enabled = authService.is2FaEnabled(auth.getName());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(enabled)
                .message(enabled ? "2FA is enabled" : "2FA is not enabled")
                .build());
    }
}
