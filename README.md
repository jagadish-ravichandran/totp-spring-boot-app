# 🔐 Spring Boot 2FA — TOTP Authentication

A production-ready **Two-Factor Authentication** (2FA) implementation built with Spring Boot. Uses the **TOTP standard (RFC 6238)** — the same protocol used by Google Authenticator, Authy, and 1Password — with secrets encrypted at rest using **AES-256-GCM**.

![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

---

## ✨ Features

- ✅ User registration and login with Spring Security
- ✅ TOTP-based 2FA (works with any RFC 6238 authenticator app)
- ✅ QR code generation for easy authenticator setup
- ✅ AES-256-GCM encryption for secrets stored in the database
- ✅ Session-based TOTP enforcement — protected routes blocked until OTP verified
- ✅ Enable / disable 2FA from the dashboard
- ✅ REST API endpoints for all auth operations
- ✅ Dark-themed, responsive UI (Thymeleaf + vanilla CSS)

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Security | Spring Security 6 |
| OTP | GoogleAuth (RFC 6238 / TOTP) |
| Encryption | AES-256-GCM (JDK `javax.crypto`) |
| QR Code | ZXing |
| Database | H2 (in-memory, swap for MySQL/PostgreSQL in prod) |
| Templating | Thymeleaf |
| Build | Maven |
| Java | 17+ |

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Clone & Run

```bash
git clone https://github.com/your-username/spring-boot-2fa.git
cd spring-boot-2fa
mvn spring-boot:run
```

Open `http://localhost:8080` in your browser.

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `TOTP_ENCRYPTION_KEY` | Base64-encoded 32-byte AES key | Dev key (see below) |

> ⚠️ **Never use the default key in production.** Generate your own:

```bash
# Generate a secure 256-bit key
python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"

# Set it
export TOTP_ENCRYPTION_KEY=<your-generated-key>

mvn spring-boot:run
```

---

## 🐳 Docker Support

Run the application in a container using Docker.

### Build the Image

```bash
docker build -t spring-boot-2fa .
```

### Run the Container

```bash
docker run -p 8080:8080 spring-boot-2fa
```

Open:

```text
http://localhost:8080
```

### Docker Notes

- Multi-stage Docker build for smaller production images
- Runs with a non-root user for improved container security
- Uses Eclipse Temurin JRE 17 runtime image
- Compatible with AMD64 and ARM64 platforms
- If `TOTP_ENCRYPTION_KEY` is not provided as an environment variable, the application falls back to the value configured in `application.properties`

---

---

## 🎥 Demo

A complete walkthrough of registration, login, TOTP setup, and OTP verification is available below.


[▶ View Demo Video](docs/demo.mp4)

### Suggested Demo Flow

1. User registration
2. Login with username/password
3. Enable 2FA
4. Scan QR code using Google Authenticator/Authy
5. Verify OTP
6. Logout and login again
7. TOTP enforcement redirect to `/verify-2fa`
8. Successful OTP verification and dashboard access

---

## 📁 Project Structure

```
src/main/java/com/auth/totp/
├── TotpApplication.java
├── config/
│   └── SecurityConfig.java              # Spring Security filter chain
├── controller/
│   ├── WebController.java               # Page routes (Thymeleaf)
│   └── ApiController.java               # REST API endpoints
├── dto/
│   └── AuthDto.java                     # Request / Response DTOs
├── entity/
│   └── User.java                        # JPA User entity
├── repository/
│   └── UserRepository.java
├── security/
│   ├── CustomUserDetailsService.java    # Loads user for Spring Security
│   ├── TotpAuthSuccessHandler.java      # Redirects to /verify-2fa after login
│   └── TotpVerificationFilter.java      # Blocks protected routes until OTP verified
├── service/
│   └── AuthService.java                 # Core 2FA business logic
└── util/
    ├── AesEncryptor.java                # AES-256-GCM encrypt / decrypt
    └── QrCodeGenerator.java             # ZXing QR code generation
```

---

## 🔄 Authentication Flow

```
1. Register         →  POST /register
2. Login            →  POST /login  (password verified by Spring Security)
                         │
                         ├─ 2FA enabled?  YES → redirect /verify-2fa
                         │                       session["TOTP_VERIFIED"] = false
                         │
                         └─ 2FA enabled?  NO  → redirect /dashboard
                                                 session["TOTP_VERIFIED"] = true
3. Verify OTP       →  POST /verify-2fa
                         │
                         └─ OTP correct? → session["TOTP_VERIFIED"] = true
                                           redirect /dashboard ✅

  TotpVerificationFilter runs on every request:
  if authenticated AND session["TOTP_VERIFIED"] == false → redirect /verify-2fa
```

---

## 🔌 REST API

All endpoints require authentication (Basic or session cookie).

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `GET` | `/api/2fa/setup` | Generate secret + QR code |
| `POST` | `/api/2fa/confirm?code=` | Confirm and enable 2FA |
| `POST` | `/api/2fa/verify?code=` | Verify OTP (post-login) |
| `POST` | `/api/2fa/disable?code=` | Disable 2FA |
| `GET` | `/api/2fa/status` | Check if 2FA is enabled |

### Example: Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"secret123"}'
```

### Example: Setup 2FA

```bash
curl -X GET http://localhost:8080/api/2fa/setup \
  -u alice:secret123
```

### Example: Verify OTP

```bash
curl -X POST "http://localhost:8080/api/2fa/verify?code=123456" \
  -u alice:secret123
```

---

## 🔒 Security Design

### Secret Storage

Each user gets a **unique TOTP secret**, generated once when they enable 2FA. It is:

1. Generated using `GoogleAuthenticator.createCredentials()`
2. Encrypted with **AES-256-GCM** before being written to the database
3. Decrypted only at the moment of OTP verification — never stored in plaintext

### Why AES-256-GCM?

| Property | Detail |
|---|---|
| Confidentiality | 256-bit AES — quantum-resistant for the foreseeable future |
| Integrity | GCM authentication tag detects any tampering |
| Uniqueness | Random 96-bit IV generated per encryption — no two ciphertexts are alike |

### Session Enforcement

A session attribute `TOTP_VERIFIED` is used to track whether the user has completed the OTP step. `TotpVerificationFilter` checks this on every request — typing `/dashboard` directly in the browser still redirects to `/verify-2fa`.

---

## 🗄 Database

Uses **H2 in-memory** database by default for easy local development.

### Switch to MySQL / PostgreSQL for production

In `application.properties`:

```properties
# Remove H2, add your DB driver to pom.xml, then:
spring.datasource.url=jdbc:postgresql://localhost:5432/totpdb
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

---

## 🧪 Compatible Authenticator Apps

| App | Platform |
|---|---|
| Google Authenticator | iOS, Android |
| Authy | iOS, Android, Desktop |
| Microsoft Authenticator | iOS, Android |
| 1Password | iOS, Android, Desktop |
| Bitwarden | iOS, Android, Desktop |

Any app supporting **RFC 6238 TOTP** will work.

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "Add my feature"`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request

Please make sure your code compiles and follows the existing package structure.

---

## 📋 Roadmap

- [ ] Backup / recovery codes
- [ ] Rate limiting on OTP attempts
- [ ] MySQL / PostgreSQL Docker Compose setup
- [ ] JWT-based stateless API authentication
- [ ] Email verification on registration
- [ ] Remember this device (30-day trusted device cookie)

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgements

- [GoogleAuth](https://github.com/wstrange/GoogleAuth) — TOTP implementation
- [ZXing](https://github.com/zxing/zxing) — QR code generation
- [Spring Security](https://spring.io/projects/spring-security) — authentication framework