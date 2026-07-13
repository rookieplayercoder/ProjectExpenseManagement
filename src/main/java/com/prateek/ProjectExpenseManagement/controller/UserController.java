package com.prateek.ProjectExpenseManagement.controller;

import com.prateek.ProjectExpenseManagement.dto.CreateUserRequest;
import com.prateek.ProjectExpenseManagement.dto.CreateUserResponse;
import com.prateek.ProjectExpenseManagement.dto.UserLookupResponse;
import com.prateek.ProjectExpenseManagement.dto.UserProfileResponse;
import com.prateek.ProjectExpenseManagement.security.AuthenticatedUser;
import com.prateek.ProjectExpenseManagement.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    /**
     * The authenticated caller's own profile, identified from the JWT
     * (see JwtAuthenticationFilter) - never from a path/query parameter,
     * so there's no way to fetch another user's profile through this route.
     */
    @GetMapping("/me")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.getMyProfile(principal.userId());
    }

    /**
     * Used by the "add member by email" flow on the frontend. Requires an exact,
     * case-normalized email match - not a search - so it can't be used to enumerate
     * users. Only reachable by an already-authenticated caller (see SecurityConfig).
     */
    @GetMapping("/lookup")
    public UserLookupResponse lookupByEmail(@RequestParam String email) {
        return userService.lookupByEmail(email.trim().toLowerCase());
    }
}