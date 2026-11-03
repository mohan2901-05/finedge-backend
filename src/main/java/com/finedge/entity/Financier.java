package com.finedge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * M2 Part C entity for {@code financiers} (lender directory).
 *
 * <p>PK {@code pk_financier_id VARCHAR(50)} is application-assigned via
 * {@link com.finedge.service.StringIdGenerator}; no {@code @GeneratedValue}
 * by design, consistent with other M1/M2 entities.
 *
 * <p>Financial figures (rates, amounts, tenure) are stored as supplied --
 * never recalculated here. {@code status} defaults to {@code ACTIVE} and is
 * used for soft deactivation (no hard delete, preserving historical
 * references).
 */
@Entity
@Table(name = "financiers")
public class Financier {

    @Id
    @Column(name = "pk_financier_id", length = 50, nullable = false, updatable = false)
    private String financierId;

    @Column(name = "institution_name", length = 100, nullable = false, unique = true)
    private String institutionName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "indicative_annual_rate", precision = 5, scale = 2)
    private BigDecimal indicativeAnnualRate;

    @Column(name = "min_loan_amount", precision = 15, scale = 2)
    private BigDecimal minLoanAmount;

    @Column(name = "max_loan_amount", precision = 15, scale = 2)
    private BigDecimal maxLoanAmount;

    @Column(name = "available_tenure_months")
    private Integer availableTenureMonths;

    @Column(name = "status", length = 10, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    @Column(name = "created_user", length = 50)
    private String createdUser;

    @Column(name = "updated_datetime", nullable = false)
    private LocalDateTime updatedDatetime;

    @Column(name = "updated_user", length = 50)
    private String updatedUser;

    public Financier() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdDatetime == null) {
            this.createdDatetime = now;
        }
        if (this.updatedDatetime == null) {
            this.updatedDatetime = now;
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDatetime = LocalDateTime.now();
    }

    public String getFinancierId() {
        return financierId;
    }

    public void setFinancierId(String financierId) {
        this.financierId = financierId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getIndicativeAnnualRate() {
        return indicativeAnnualRate;
    }

    public void setIndicativeAnnualRate(BigDecimal indicativeAnnualRate) {
        this.indicativeAnnualRate = indicativeAnnualRate;
    }

    public BigDecimal getMinLoanAmount() {
        return minLoanAmount;
    }

    public void setMinLoanAmount(BigDecimal minLoanAmount) {
        this.minLoanAmount = minLoanAmount;
    }

    public BigDecimal getMaxLoanAmount() {
        return maxLoanAmount;
    }

    public void setMaxLoanAmount(BigDecimal maxLoanAmount) {
        this.maxLoanAmount = maxLoanAmount;
    }

    public Integer getAvailableTenureMonths() {
        return availableTenureMonths;
    }

    public void setAvailableTenureMonths(Integer availableTenureMonths) {
        this.availableTenureMonths = availableTenureMonths;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(LocalDateTime createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public String getCreatedUser() {
        return createdUser;
    }

    public void setCreatedUser(String createdUser) {
        this.createdUser = createdUser;
    }

    public LocalDateTime getUpdatedDatetime() {
        return updatedDatetime;
    }

    public void setUpdatedDatetime(LocalDateTime updatedDatetime) {
        this.updatedDatetime = updatedDatetime;
    }

    public String getUpdatedUser() {
        return updatedUser;
    }

    public void setUpdatedUser(String updatedUser) {
        this.updatedUser = updatedUser;
    }
}
