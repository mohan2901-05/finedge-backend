package com.finedge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * User update. Password and PAN cannot be changed through this API —
 * there is intentionally no password-rotation flow until authentication
 * requirements are confirmed.
 */
public class UserUpdateRequest {

    @Size(max = 50, message = "User name must be at most 50 characters")
    private String userName;

    @Email(message = "User email must be a valid format")
    @Size(max = 50, message = "User email must be at most 50 characters")
    private String userEmail;

    private LocalDate dob;

    private String roleId;

    @Size(max = 10, message = "Status must be at most 10 characters")
    private String status;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
