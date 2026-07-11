package com.prateek.ProjectExpenseManagement.controller;

import com.prateek.ProjectExpenseManagement.dto.CreateUserRequest;
import com.prateek.ProjectExpenseManagement.dto.CreateUserResponse;
import com.prateek.ProjectExpenseManagement.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
}
