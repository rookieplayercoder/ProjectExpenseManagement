package com.prateek.ProjectExpenseManagement.service;

import com.prateek.ProjectExpenseManagement.dto.CreateUserRequest;
import com.prateek.ProjectExpenseManagement.dto.CreateUserResponse;
import com.prateek.ProjectExpenseManagement.dto.UserLookupResponse;
import com.prateek.ProjectExpenseManagement.dto.UserProfileResponse;
import com.prateek.ProjectExpenseManagement.exception.BusinessValidationException;
import com.prateek.ProjectExpenseManagement.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserProfileResponse getMyProfile(UUID userId) {
        return userRepository.findProfileById(userId);
    }

    public UserLookupResponse lookupByEmail(String email) {
        return userRepository.findLookupByEmail(email);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public CreateUserResponse createUser(@Valid CreateUserRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        request.setEmail(normalizedEmail);

        userRepository.assertEmailNotInUse(normalizedEmail);

        String passwordHash = passwordEncoder.encode(request.getPassword());

        UUID userId;
        try {
            userId = userRepository.insertUser(request, passwordHash);
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent signups can both pass the check above; the DB's unique
            // constraint on email is the real source of truth here.
            throw new BusinessValidationException("Email is already registered: " + normalizedEmail);
        }

        return new CreateUserResponse(userId, "SUCCESS", "User created successfully");
    }
}
