package com.finedge.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request to record a repayment against a disbursed loan application.
 *
 * <p>The repayment amount must be positive and must not exceed the
 * outstanding balance; the service enforces both rules on the backend.
 */
public class RepaymentCreateRequest {

    @NotNull(message = "Repayment amount is required")
    @Positive(message = "Repayment amount must be greater than zero")
    private BigDecimal repaymentAmount;

    public BigDecimal getRepaymentAmount() {
        return repaymentAmount;
    }

    public void setRepaymentAmount(BigDecimal repaymentAmount) {
        this.repaymentAmount = repaymentAmount;
    }
}
