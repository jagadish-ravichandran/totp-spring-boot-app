package com.auth.totp.dto;

import jakarta.validation.constraints.*;
import lombok.*;

public class AuthDto {

    @Getter @Setter
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 30)
        private String username;

        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Size(min = 6)
        private String password;
    }

    @Getter @Setter
    public static class LoginRequest {
        @NotBlank
        private String username;

        @NotBlank
        private String password;
    }

    @Getter @Setter
    public static class TotpVerifyRequest {
        @NotBlank
        private String username;

        @NotNull
        @Min(100000) @Max(999999)
        private Integer code;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Setup2FaResponse {
        private String qrCodeBase64;
        private String secret;
        private String otpAuthUrl;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ApiResponse {
        private boolean success;
        private String message;
    }
}
