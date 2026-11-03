package com.finedge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for {@code loan_repayments} (Roadmap G).
 *
 * <p>Each repayment is an individual row against a disbursed loan
 * application. {@code pk_repayment_id} is application-assigned and stable.
 * Money is stored as {@code DECIMAL(15,2)} (rounded to 2 decimals, {@link
 * java.math.RoundingMode#HALF_UP}) by the service layer. The outstanding
 * balance is computed on the backend from the disbursed amount minus the
 * sum of persisted repayments; no balance column is stored here.
 */
@Entity
@Table(name = "loan_repayments")
public class LoanRepayment {

    @Id
    @Column(name = "pk_repayment_id", length = 50, nullable = false, updatable = false)
    private String repaymentId;

    @Column(name = "fk_loan_application_id", nullable = false)
    private Integer applicationId;

    @Column(name = "repayment_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal repaymentAmount;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "paid_by_user_id", length = 50, nullable = false)
    private String paidByUserId;

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    public LoanRepayment() {
    }

    public String getRepaymentId() {
        return repaymentId;
    }

    public void setRepaymentId(String repaymentId) {
        this.repaymentId = repaymentId;
    }

    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    public BigDecimal getRepaymentAmount() {
        return repaymentAmount;
    }

    public void setRepaymentAmount(BigDecimal repaymentAmount) {
        this.repaymentAmount = repaymentAmount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getPaidByUserId() {
        return paidByUserId;
    }

    public void setPaidByUserId(String paidByUserId) {
        this.paidByUserId = paidByUserId;
    }

    public LocalDateTime getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(LocalDateTime createdDatetime) {
        this.createdDatetime = createdDatetime;
    }
}
