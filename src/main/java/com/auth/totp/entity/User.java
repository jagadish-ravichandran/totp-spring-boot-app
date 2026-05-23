package com.auth.totp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "totp_secret")
    private String totpSecret;          // AES-256-GCM encrypted

    @Column(name = "two_fa_enabled")
    private boolean twoFaEnabled;

    @Column(name = "role")
    private String role;
}
