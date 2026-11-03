package com.finedge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * User creation. {@code password} is write-only: stored as received, never
 * returned or logged. No hashing or strength rules are applied here —
 * authentication design is an open gap (see report), so this API must not
 * be mistaken for a credential-issuing flow.
 */
public class UserCreateRequest {

    @NotBlank(message = "Organization ID is required")
    private String orgId;

    @NotBlank(message = "Role ID is required")
    private String roleId;

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

    @Size(max = 10, message = "Status must be at most 10 characters")
    private String status;

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getPanNo() {
        return panNo;
    }

    public void setPanNo(String panNo) {
        this.panNo = panNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
