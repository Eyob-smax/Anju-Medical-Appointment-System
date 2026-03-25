package com.anju.domain.auth;

import com.anju.common.Result;
import com.anju.domain.auth.dto.LoginRequest;
import com.anju.domain.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication and account endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Creates a new user account with optional role and secondary password.")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return Result.success(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Validates credentials and returns account identity details.")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request);
        String basicToken = Base64.getEncoder().encodeToString(
            (request.getUsername() + ":" + request.getPassword()).getBytes(StandardCharsets.UTF_8)
        );
        return Result.success(Map.of(
                "username", user.getUsername(),
                "role", user.getRole(),
            "authorization", "Basic " + basicToken,
            "message", "Login success. Send the authorization value in every protected request."
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Returns current authenticated username.")
    public Result<Map<String, Object>> me(Authentication authentication) {
        return Result.success(Map.of("username", authentication.getName()));
    }

    @PostMapping("/verify-secondary")
    @Operation(summary = "Verify secondary password", description = "Validates X-Secondary-Password for sensitive operations.")
    public Result<Map<String, Object>> verifySecondary(
            Authentication authentication,
            @RequestHeader("X-Secondary-Password") String secondaryPassword) {
        authService.verifySecondaryPassword(authentication.getName(), secondaryPassword);
        return Result.success(Map.of("verified", true));
    }
}
