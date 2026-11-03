package com.finedge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuditLogCreateRequest {

    @NotBlank(message = "Organization ID is required")
    private String orgId;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "User name is required")
    @Size(max = 50, message = "User name must be at most 50 characters")
    private String userName;

    @NotBlank(message = "Action is required")
    @Size(max = 50, message = "Action must be at most 50 characters")
    private String action;

    @Size(max = 500, message = "Details must be at most 500 characters")
    private String details;

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
