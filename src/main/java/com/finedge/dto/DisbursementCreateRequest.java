package com.finedge.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request to record a disbursement for an approved loan application.
 *
 * <p>The disbursed amount is a positive {@link BigDecimal}; no fees,
 * deductions, or financial rules are applied by the API.
 */
public class DisbursementCreateRequest {

    @NotNull(message = "Disbursed amount is required")
    @Positive(message = "Disbursed amount must be greater than zero")
    private BigDecimal disbursedAmount;

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }
}
