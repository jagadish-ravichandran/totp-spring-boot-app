package com.auth.totp.controller;

import com.auth.totp.dto.AuthDto.*;
import com.auth.totp.entity.User;
import com.auth.totp.security.TotpAuthSuccessHandler;
import com.auth.totp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final AuthService authService;

    @GetMapping("/")
    public String home() { return "redirect:/dashboard"; }

    // ─── Registration ─────────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest request,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (result.hasErrors()) return "register";

        ApiResponse response = authService.register(request);
        if (response.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", "Account created! Please log in.");
            return "redirect:/login";
        }
        model.addAttribute("error", response.getMessage());
        return "register";
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error  != null) model.addAttribute("error",   "Invalid username or password.");
        if (logout != null) model.addAttribute("success", "You have been logged out.");
        return "login";
    }

    // ─── Dashboard ───────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = authService.getUser(auth.getName());
        model.addAttribute("user",         user);
        model.addAttribute("twoFaEnabled", user.isTwoFaEnabled());
        return "dashboard";
    }

    // ─── 2FA Setup ───────────────────────────────────────────────────────────

    @GetMapping("/setup-2fa")
    public String setup2FaPage(Authentication auth, Model model) {
        Setup2FaResponse setup = authService.setup2Fa(auth.getName());
        model.addAttribute("qrCode",   setup.getQrCodeBase64());
        model.addAttribute("secret",   setup.getSecret());
        model.addAttribute("username", auth.getName());
        return "setup-2fa";
    }

    @PostMapping("/confirm-2fa")
    public String confirm2Fa(Authentication auth,
                             @RequestParam int code,
                             RedirectAttributes redirectAttributes) {
        ApiResponse response = authService.confirm2Fa(auth.getName(), code);
        if (response.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", "2FA has been enabled on your account!");
        } else {
            redirectAttributes.addFlashAttribute("error", response.getMessage());
            return "redirect:/setup-2fa";
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/disable-2fa")
    public String disable2Fa(Authentication auth,
                             @RequestParam int code,
                             RedirectAttributes redirectAttributes) {
        ApiResponse response = authService.disable2Fa(auth.getName(), code);
        if (response.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", "2FA has been disabled.");
        } else {
            redirectAttributes.addFlashAttribute("error", response.getMessage());
        }
        return "redirect:/dashboard";
    }

    // ─── 2FA Verify Page (post-login) ────────────────────────────────────────

    @GetMapping("/verify-2fa")
    public String verify2FaPage(Authentication auth, Model model) {
        if (auth != null) model.addAttribute("username", auth.getName());
        return "verify-2fa";
    }

    @PostMapping("/verify-2fa")
    public String verify2Fa(Authentication auth,
                            @RequestParam int code,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        ApiResponse response = authService.verify2Fa(auth.getName(), code);
        if (response.isSuccess()) {
            // ✅ Mark TOTP as verified in the session — filter will now allow access
            request.getSession().setAttribute(
                    TotpAuthSuccessHandler.TOTP_VERIFIED_SESSION_KEY, true);
            return "redirect:/dashboard";
        }
        redirectAttributes.addFlashAttribute("error", "Invalid OTP. Please try again.");
        return "redirect:/verify-2fa";
    }
}