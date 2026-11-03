package com.finedge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ModuleCreateRequest {

    @NotBlank(message = "Module name is required")
    @Size(max = 50, message = "Module name must be at most 50 characters")
    private String moduleName;

    @NotBlank(message = "Module code is required")
    @Size(max = 50, message = "Module code must be at most 50 characters")
    private String moduleCode;

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }
}
