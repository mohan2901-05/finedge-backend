package com.finedge.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response reflecting a persisted disbursement. JPA entities are never
 * exposed directly.
 */
public class DisbursementDTO {

    private String disbursementId;
    private Integer applicationId;
    private BigDecimal disbursedAmount;
    private BigDecimal loanAmount;
    private LocalDateTime disbursedAt;
    private String disbursedByUserId;
    private LocalDateTime createdDatetime;

    public DisbursementDTO() {
    }

    public String getDisbursementId() {
        return disbursementId;
    }

    public void setDisbursementId(String disbursementId) {
        this.disbursementId = disbursementId;
    }

    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public void setDisbursedAt(LocalDateTime disbursedAt) {
        this.disbursedAt = disbursedAt;
    }

    public String getDisbursedByUserId() {
        return disbursedByUserId;
    }

    public void setDisbursedByUserId(String disbursedByUserId) {
        this.disbursedByUserId = disbursedByUserId;
    }

    public LocalDateTime getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(LocalDateTime createdDatetime) {
        this.createdDatetime = createdDatetime;
    }
}
