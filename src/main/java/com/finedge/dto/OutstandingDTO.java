package com.finedge.dto;

import java.math.BigDecimal;

/**
 * Response reflecting the outstanding balance of a disbursed loan
 * application, computed on the backend from persisted records.
 */
public class OutstandingDTO {

    private Integer applicationId;
    private boolean disbursed;
    private BigDecimal disbursedAmount;
    private BigDecimal totalRepaid;
    private BigDecimal outstandingAmount;
    private boolean fullyRepaid;

    public OutstandingDTO() {
    }

    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    public boolean isDisbursed() {
        return disbursed;
    }

    public void setDisbursed(boolean disbursed) {
        this.disbursed = disbursed;
    }

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }

    public BigDecimal getTotalRepaid() {
        return totalRepaid;
    }

    public void setTotalRepaid(BigDecimal totalRepaid) {
        this.totalRepaid = totalRepaid;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public boolean isFullyRepaid() {
        return fullyRepaid;
    }

    public void setFullyRepaid(boolean fullyRepaid) {
        this.fullyRepaid = fullyRepaid;
    }
}
