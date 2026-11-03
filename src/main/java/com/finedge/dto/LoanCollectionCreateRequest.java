package com.finedge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LoanCollectionCreateRequest {

    @NotBlank(message = "Organization ID is required")
    private String orgId;

    @NotBlank(message = "Loan ID is required")
    private String loanId;

    @NotBlank(message = "User ID (payer) is required")
    private String userId;

    @NotNull(message = "Collection amount is required")
    private BigDecimal collectionAmount;

    private LocalDate collectionDate;

    @Size(max = 20, message = "Payment mode must be at most 20 characters")
    private String paymentMode;

    @Size(max = 100, message = "Reference number must be at most 100 characters")
    private String referenceNo;

    private String collectedByUserId;

    @Size(max = 255, message = "Remarks must be at most 255 characters")
    private String remarks;

    @Size(max = 20, message = "Status must be at most 20 characters")
    private String status;

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getCollectionAmount() {
        return collectionAmount;
    }

    public void setCollectionAmount(BigDecimal collectionAmount) {
        this.collectionAmount = collectionAmount;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(LocalDate collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getCollectedByUserId() {
        return collectedByUserId;
    }

    public void setCollectedByUserId(String collectedByUserId) {
        this.collectedByUserId = collectedByUserId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
