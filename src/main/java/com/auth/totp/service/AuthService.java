package com.auth.totp.service;

import com.auth.totp.dto.AuthDto.*;
import com.auth.totp.entity.User;
import com.auth.totp.repository.UserRepository;
import com.auth.totp.util.AesEncryptor;
import com.auth.totp.util.QrCodeGenerator;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final AesEncryptor      aesEncryptor;
    private final QrCodeGenerator   qrCodeGenerator;
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    @Value("${app.totp.issuer}")
    private String issuer;

    // ─── Registration ────────────────────────────────────────────────────────

    public ApiResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            return ApiResponse.builder().success(false).message("Username already taken").build();

        if (userRepository.existsByEmail(request.getEmail()))
            return ApiResponse.builder().success(false).message("Email already registered").build();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .twoFaEnabled(false)
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
        return ApiResponse.builder().success(true).message("Registration successful").build();
    }

    // ─── 2FA Setup ───────────────────────────────────────────────────────────

    public Setup2FaResponse setup2Fa(String username) {
        User user = getUser(username);

        // Generate a new TOTP secret for this user
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String rawSecret = key.getKey();

        // Encrypt & persist
        user.setTotpSecret(aesEncryptor.encrypt(rawSecret));
        user.setTwoFaEnabled(false); // not confirmed yet
        userRepository.save(user);

        // Build OTP Auth URL for QR code
        String otpAuthUrl = GoogleAuthenticatorQRGenerator
                .getOtpAuthTotpURL(issuer, username, key);

        String qrBase64 = qrCodeGenerator.generateQrCodeBase64(otpAuthUrl);

        return Setup2FaResponse.builder()
                .qrCodeBase64(qrBase64)
                .secret(rawSecret)           // shown once for manual entry
                .otpAuthUrl(otpAuthUrl)
                .build();
    }

    // ─── 2FA Confirm / Enable ────────────────────────────────────────────────

    public ApiResponse confirm2Fa(String username, int code) {
        User user = getUser(username);

        if (user.getTotpSecret() == null)
            return ApiResponse.builder().success(false).message("2FA not set up yet").build();

        String rawSecret = aesEncryptor.decrypt(user.getTotpSecret());

        if (!gAuth.authorize(rawSecret, code))
            return ApiResponse.builder().success(false).message("Invalid OTP — try again").build();

        user.setTwoFaEnabled(true);
        userRepository.save(user);
        return ApiResponse.builder().success(true).message("2FA enabled successfully").build();
    }

    // ─── 2FA Verify (Login step) ─────────────────────────────────────────────

    public ApiResponse verify2Fa(String username, int code) {
        User user = getUser(username);

        if (!user.isTwoFaEnabled() || user.getTotpSecret() == null)
            return ApiResponse.builder().success(false).message("2FA is not enabled for this user").build();

        String rawSecret = aesEncryptor.decrypt(user.getTotpSecret());

        boolean valid = gAuth.authorize(rawSecret, code);
        return ApiResponse.builder()
                .success(valid)
                .message(valid ? "OTP verified" : "Invalid OTP")
                .build();
    }

    // ─── Disable 2FA ─────────────────────────────────────────────────────────

    public ApiResponse disable2Fa(String username, int code) {
        User user = getUser(username);

        String rawSecret = aesEncryptor.decrypt(user.getTotpSecret());
        if (!gAuth.authorize(rawSecret, code))
            return ApiResponse.builder().success(false).message("Invalid OTP").build();

        user.setTotpSecret(null);
        user.setTwoFaEnabled(false);
        userRepository.save(user);
        return ApiResponse.builder().success(true).message("2FA disabled").build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public boolean is2FaEnabled(String username) {
        return userRepository.findByUsername(username)
                .map(User::isTwoFaEnabled)
                .orElse(false);
    }
}
