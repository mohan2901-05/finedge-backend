package com.finedge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OrganizationCreateRequest {

    @NotBlank(message = "Organization name is required")
    @Size(max = 100, message = "Organization name must be at most 100 characters")
    private String orgName;

    @NotBlank(message = "Organization email is required")
    @Email(message = "Organization email must be a valid format")
    @Size(max = 50, message = "Organization email must be at most 50 characters")
    private String orgEmail;

    @Size(max = 10, message = "Status must be at most 10 characters")
    private String status;

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getOrgEmail() {
        return orgEmail;
    }

    public void setOrgEmail(String orgEmail) {
        this.orgEmail = orgEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
