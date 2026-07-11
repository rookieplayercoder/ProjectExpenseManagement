package com.prateek.ProjectExpenseManagement.controller;

import com.prateek.ProjectExpenseManagement.dto.LoginRequest;
import com.prateek.ProjectExpenseManagement.dto.LoginResponse;
import com.prateek.ProjectExpenseManagement.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
