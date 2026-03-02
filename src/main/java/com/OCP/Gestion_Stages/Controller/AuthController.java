package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.domain.dto.auth.AuthResponse;
import com.OCP.Gestion_Stages.domain.dto.auth.LoginRequest;
import com.OCP.Gestion_Stages.domain.dto.user.UserRequest;
import com.OCP.Gestion_Stages.Service.interfaces.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
}