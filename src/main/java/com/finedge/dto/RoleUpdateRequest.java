package com.finedge.dto;

import jakarta.validation.constraints.Size;

public class RoleUpdateRequest {

    @Size(max = 50, message = "Role name must be at most 50 characters")
    private String roleName;

    @Size(max = 255, message = "Role description must be at most 255 characters")
    private String roleDescription;

    @Size(max = 10, message = "Status must be at most 10 characters")
    private String status;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public void setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
