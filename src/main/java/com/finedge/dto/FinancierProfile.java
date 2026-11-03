package com.finedge.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Financier directory response. Never exposes internal persistence details;
 * field names match the Angular {@code FinancierProfile} model exactly.
 */
public class FinancierProfile {

    private String financierId;
    private String institutionName;
    private String description;
    private BigDecimal indicativeAnnualRate;
    private BigDecimal minLoanAmount;
    private BigDecimal maxLoanAmount;
    private Integer availableTenureMonths;
    private String status;
    private LocalDateTime createdDatetime;
    private LocalDateTime updatedDatetime;

    public FinancierProfile() {
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

    public LocalDateTime getUpdatedDatetime() {
        return updatedDatetime;
    }

    public void setUpdatedDatetime(LocalDateTime updatedDatetime) {
        this.updatedDatetime = updatedDatetime;
    }
}
