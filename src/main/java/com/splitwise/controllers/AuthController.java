package com.splitwise.controllers;

import com.splitwise.dto.request.AuthRequest;
import com.splitwise.dto.request.RegisterRequest;
import com.splitwise.dto.response.AuthResponse;
import com.splitwise.services.AuthService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/google")
    public ResponseEntity<Map<String, Object>> googleLogin() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Google OAuth is not configured on backend yet");
        response.put("status", HttpStatus.NOT_IMPLEMENTED.value());
        response.put("path", "/api/auth/google");
        response.put("message", "Configure Google OAuth provider and callback endpoint in backend");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
    }
}
