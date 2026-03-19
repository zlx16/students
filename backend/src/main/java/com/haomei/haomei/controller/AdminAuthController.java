package com.haomei.haomei.controller;

import com.haomei.haomei.dto.AdminLoginRequest;
import com.haomei.haomei.dto.AdminLoginResponse;
import com.haomei.haomei.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {
    private final JwtService jwtService;
    private final String adminUsername;
    private final String adminPasswordEncoded;

    public AdminAuthController(
            JwtService jwtService,
            @Value("${app.admin.username}") String adminUsername,
            @Value("${app.admin.password}") String adminPasswordEncoded
    ) {
        this.jwtService = jwtService;
        this.adminUsername = adminUsername;
        this.adminPasswordEncoded = adminPasswordEncoded;
    }

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AdminLoginRequest req) {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        boolean ok = adminUsername.equals(req.username()) && encoder.matches(req.password(), adminPasswordEncoded);
        if (!ok) {
            return ResponseEntity.status(401).build();
        }
        String token = jwtService.createToken(adminUsername);
        return ResponseEntity.ok(new AdminLoginResponse(token));
    }
}

