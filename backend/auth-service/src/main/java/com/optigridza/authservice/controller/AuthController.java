package com.optigridza.authservice.controller;

import com.optigridza.authservice.dto.AuthResponse;
import com.optigridza.authservice.dto.LoginRequest;
import com.optigridza.authservice.dto.RegisterRequest;
import com.optigridza.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private static final String AUTH_COOKIE = "OPTIGRID_AUTH";

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${auth.cookie.secure:false}")
    private boolean secureCookie;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            Authentication authentication) {
        AuthResponse response = authService.register(request);
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return authResponse(response);
        }
        response.setToken(null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        return authResponse(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> currentUser(Authentication authentication) {
        return ResponseEntity.ok(authService.currentUser(authentication.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PatchMapping("/users/{email}/company")
    public ResponseEntity<Void> updateUserCompany(@PathVariable String email,
                                                  @RequestBody Map<String, String> body){
        authService.updateUserCompany(email, body.get("companyId"));
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<AuthResponse> authResponse(AuthResponse response) {
        ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE, response.getToken())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(jwtExpiration))
                .build();
        response.setToken(null);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
