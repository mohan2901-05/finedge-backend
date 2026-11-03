package com.finedge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for {@code loan_disbursements} (Roadmap G).
 *
 * <p>One disbursement per loan application: {@code
 * fk_loan_application_id} is unique, so a loan cannot be disbursed twice.
 * {@code pk_disbursement_id} is application-assigned. Money is stored as
 * {@code DECIMAL(15,2)} (rounded to 2 decimals, {@link
 * java.math.RoundingMode#HALF_UP}) by the service layer. No JPA cascade;
 * the application row is loaded and owned separately.
 */
@Entity
@Table(name = "loan_disbursements")
public class LoanDisbursement {

    @Id
    @Column(name = "pk_disbursement_id", length = 50, nullable = false, updatable = false)
    private String disbursementId;

    @Column(name = "fk_loan_application_id", nullable = false)
    private Integer applicationId;

    @Column(name = "disbursed_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal disbursedAmount;

    @Column(name = "disbursed_at", nullable = false)
    private LocalDateTime disbursedAt;

    @Column(name = "disbursed_by_user_id", length = 50, nullable = false)
    private String disbursedByUserId;

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    public LoanDisbursement() {
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
