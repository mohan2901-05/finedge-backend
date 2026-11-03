package com.finedge.dto;

import jakarta.validation.constraints.Size;

public class PermissionUpdateRequest {

    @Size(max = 50, message = "Permission name must be at most 50 characters")
    private String permissionName;

    @Size(max = 50, message = "Permission code must be at most 50 characters")
    private String permissionCode;

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }
}
