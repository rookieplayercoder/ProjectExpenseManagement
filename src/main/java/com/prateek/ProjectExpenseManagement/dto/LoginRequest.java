package com.prateek.ProjectExpenseManagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank
    @Email
    @Size(max = 320)
    private String email;

    // Deliberately no @Size(min=...) here - a login attempt with a too-short
    // password should still fail with "invalid credentials", not leak that
    // the password itself was malformed before we even check it. The max
    // just caps payload size on this unauthenticated endpoint.
    @NotBlank
    @Size(max = 72)
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
