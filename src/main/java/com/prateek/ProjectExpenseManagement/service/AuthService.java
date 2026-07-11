package com.prateek.ProjectExpenseManagement.service;

import com.prateek.ProjectExpenseManagement.domain.AuthUserView;
import com.prateek.ProjectExpenseManagement.dto.LoginRequest;
import com.prateek.ProjectExpenseManagement.dto.LoginResponse;
import com.prateek.ProjectExpenseManagement.exception.InvalidCredentialsException;
import com.prateek.ProjectExpenseManagement.repository.UserRepository;
import com.prateek.ProjectExpenseManagement.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Optional<AuthUserView> maybeUser = userRepository.findByEmail(normalizedEmail);

        // Same generic error whether the email doesn't exist or the password is
        // wrong, so we don't leak which emails are registered.
        AuthUserView user = maybeUser
                .filter(AuthUserView::active)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.passwordHash() == null || !passwordEncoder.matches(request.getPassword(), user.passwordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.id(), user.email(), user.role());
        return new LoginResponse(token, user.id(), user.email(), user.role());
    }
}
