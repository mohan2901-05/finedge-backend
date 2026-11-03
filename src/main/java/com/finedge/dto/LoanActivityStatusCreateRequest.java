package com.finedge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoanActivityStatusCreateRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Loan ID is required")
    private String loanId;

    @NotBlank(message = "Activity code is required")
    @Size(max = 50, message = "Activity code must be at most 50 characters")
    private String activityCode;

    @NotBlank(message = "Status code is required")
    @Size(max = 1, message = "Status code must be a single character")
    private String statusCode;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public String getActivityCode() {
        return activityCode;
    }

    public void setActivityCode(String activityCode) {
        this.activityCode = activityCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }
}
