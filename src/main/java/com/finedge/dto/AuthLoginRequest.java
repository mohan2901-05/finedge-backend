package com.finedge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login credentials. Authentication is performed against the stored BCrypt
 * hash; failure returns a generic response regardless of whether the email
 * exists.
 */
public class AuthLoginRequest {

    @NotBlank(message = "User email is required")
    @Email(message = "User email must be a valid format")
    @Size(max = 50, message = "User email must be at most 50 characters")
    private String userEmail;

    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Password must be at most 255 characters")
    private String password;

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
