package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.JwtAuthenticationResponse;
import com.ap101gamestudio.timetracker.dto.LoginRequest;
import com.ap101gamestudio.timetracker.dto.RegisterRequest;
import com.ap101gamestudio.timetracker.dto.GenerateLinkCodeResponse;
import com.ap101gamestudio.timetracker.dto.LinkAccountRequest;
import com.ap101gamestudio.timetracker.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService service;

    public AuthController(AuthenticationService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<JwtAuthenticationResponse> register(
            @RequestBody @Valid RegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<JwtAuthenticationResponse> authenticate(
            @RequestBody @Valid LoginRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("/generate-link-code")
    public ResponseEntity<GenerateLinkCodeResponse> generateLinkCode(Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(service.generateLinkCode(userEmail));
    }

    @PostMapping("/link-account")
    public ResponseEntity<JwtAuthenticationResponse> linkAccount(
            @RequestBody @Valid LinkAccountRequest request
    ) {
        return ResponseEntity.ok(service.linkAccount(request));
    }
}