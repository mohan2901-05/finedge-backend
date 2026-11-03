package com.finedge.service;

import com.finedge.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Server-side financial calculations for loan applications.
 *
 * <p>All monetary figures are calculated using {@link BigDecimal} with
 * {@link RoundingMode#HALF_UP} rounding to 2 decimal places, the standard
 * for currency. Client-supplied EMI, total interest, and total payable are
 * never trusted — all three are recalculated on the server.
 *
 * <p>Uses the reducing-balance EMI formula:
 * <ul>
 *   <li>Monthly rate r = annualRate / 12 / 100</li>
 *   <li>If r = 0: EMI = principal / tenureMonths</li>
 *   <li>If r &gt; 0: EMI = P × r × (1 + r)^n / ((1 + r)^n − 1)</li>
 *   <li>Total payable = EMI × n</li>
 *   <li>Total interest = Total payable − principal</li>
 * </ul>
 *
 * <p>Validation limits (consistent with the frontend EMI calculator):
 * principal ₹0–₹10 crore, annual rate 0–100%, tenure 1–360 months.
 */
@Service
public class EmiCalculationService {

    private static final MathContext MATH_CTX = new MathContext(30);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    private static final BigDecimal MAX_PRINCIPAL = new BigDecimal("100000000.00"); // ₹10 crore
    private static final BigDecimal MAX_RATE = new BigDecimal("100.00"); // 100% per annum
    private static final int MAX_TENURE_MONTHS = 360; // 30 years

    /**
     * Validates and calculates EMI, total interest, and total payable.
     *
     * @param loanAmount      loan principal (must be &gt; 0 and &le; ₹10 crore)
     * @param annualRate      annual interest rate in percent (must be 0–100)
     * @param tenureMonths    loan tenure in months (must be 1–360)
     * @return calculation result with EMI, total interest, and total payable
     * @throws BusinessRuleException if any input is invalid
     */
    public EmiCalculationResult calculate(BigDecimal loanAmount, BigDecimal annualRate, Integer tenureMonths) {
        validateInputs(loanAmount, annualRate, tenureMonths);

        BigDecimal principal = loanAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = annualRate.setScale(2, RoundingMode.HALF_UP);
        int n = tenureMonths;

        BigDecimal monthlyRate = rate.divide(TWELVE, MATH_CTX).divide(HUNDRED, MATH_CTX);

        BigDecimal emi;
        BigDecimal totalInterest;
        BigDecimal totalPayable;
        if (monthlyRate.compareTo(ZERO) == 0) {
            // Zero interest: simple division. Total interest is exactly zero;
            // total payable equals principal. The EMI is rounded, so the
            // final payment may differ slightly — this is normal for loans.
            emi = principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
            totalInterest = ZERO.setScale(2, RoundingMode.HALF_UP);
            totalPayable = principal;
        } else {
            // Reducing-balance formula: EMI = P × r × (1 + r)^n / ((1 + r)^n − 1)
            BigDecimal onePlusRate = ONE.add(monthlyRate);
            BigDecimal factor = onePlusRate.pow(n, MATH_CTX);
            BigDecimal numerator = principal.multiply(monthlyRate, MATH_CTX).multiply(factor, MATH_CTX);
            BigDecimal denominator = factor.subtract(ONE, MATH_CTX);
            emi = numerator.divide(denominator, 2, RoundingMode.HALF_UP);
            totalPayable = emi.multiply(BigDecimal.valueOf(n)).setScale(2, RoundingMode.HALF_UP);
            totalInterest = totalPayable.subtract(principal).setScale(2, RoundingMode.HALF_UP);
        }

        return new EmiCalculationResult(emi, totalInterest, totalPayable);
    }

    private void validateInputs(BigDecimal loanAmount, BigDecimal annualRate, Integer tenureMonths) {
        if (loanAmount == null || loanAmount.compareTo(ZERO) <= 0) {
            throw new BusinessRuleException("Loan amount must be greater than zero.");
        }
        if (loanAmount.compareTo(MAX_PRINCIPAL) > 0) {
            throw new BusinessRuleException(
                    "Loan amount must not exceed ₹1,00,00,000.00 (₹10 crore).");
        }
        if (annualRate == null || annualRate.compareTo(ZERO) < 0) {
            throw new BusinessRuleException("Interest rate must not be negative.");
        }
        if (annualRate.compareTo(MAX_RATE) > 0) {
            throw new BusinessRuleException("Interest rate must not exceed 100 percent per annum.");
        }
        if (tenureMonths == null || tenureMonths <= 0) {
            throw new BusinessRuleException("Tenure must be greater than zero months.");
        }
        if (tenureMonths > MAX_TENURE_MONTHS) {
            throw new BusinessRuleException("Tenure must not exceed 360 months (30 years).");
        }
    }

    /**
     * Result of an EMI calculation.
     */
    public record EmiCalculationResult(
            BigDecimal emi,
            BigDecimal totalInterest,
            BigDecimal totalPayable
    ) {
    }
}
