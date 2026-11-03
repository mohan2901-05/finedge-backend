package com.finedge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Self-service registration request. Deliberately contains no organisation or
 * role fields: the server assigns the configured default borrower role and
 * organisation, so a client can never self-assign administrator or financier
 * privileges.
 */
public class AuthRegisterRequest {

    @NotBlank(message = "User name is required")
    @Size(max = 50, message = "User name must be at most 50 characters")
    private String userName;

    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Password must be at most 255 characters")
    private String password;

    @NotBlank(message = "User email is required")
    @Email(message = "User email must be a valid format")
    @Size(max = 50, message = "User email must be at most 50 characters")
    private String userEmail;

    private LocalDate dob;

    @Size(max = 20, message = "PAN number must be at most 20 characters")
    private String panNo;

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getPanNo() { return panNo; }
    public void setPanNo(String panNo) { this.panNo = panNo; }
}
