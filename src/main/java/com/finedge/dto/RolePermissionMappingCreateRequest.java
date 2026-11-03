package com.finedge.dto;

import jakarta.validation.constraints.NotBlank;

public class RolePermissionMappingCreateRequest {

    @NotBlank(message = "Organization ID is required")
    private String orgId;

    @NotBlank(message = "Role ID is required")
    private String roleId;

    @NotBlank(message = "Module ID is required")
    private String moduleId;

    @NotBlank(message = "Permission ID is required")
    private String permissionId;

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

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId;
    }
}
