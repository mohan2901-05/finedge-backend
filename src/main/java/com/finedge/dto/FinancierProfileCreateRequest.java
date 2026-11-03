package com.finedge.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Financier creation request. {@code financierId} is not accepted from the
 * client -- it is assigned server-side by {@link
 * com.finedge.service.StringIdGenerator}.
 */
public class FinancierProfileCreateRequest {

    @NotBlank(message = "Institution name is required")
    @Size(max = 100, message = "Institution name must be at most 100 characters")
    private String institutionName;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @DecimalMin(value = "0.00", message = "Indicative annual rate must not be negative")
    @DecimalMax(value = "100.00", message = "Indicative annual rate must not exceed 100%")
    private BigDecimal indicativeAnnualRate;

    @DecimalMin(value = "0.00", message = "Minimum loan amount must not be negative")
    private BigDecimal minLoanAmount;

    @DecimalMin(value = "0.00", message = "Maximum loan amount must not be negative")
    private BigDecimal maxLoanAmount;

    @DecimalMin(value = "1", message = "Available tenure months must be at least 1")
    private Integer availableTenureMonths;

    @Size(max = 10, message = "Status must be at most 10 characters")
    private String status;

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
}
