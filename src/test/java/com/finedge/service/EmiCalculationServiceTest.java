package com.finedge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finedge.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class EmiCalculationServiceTest {

    private final EmiCalculationService service = new EmiCalculationService();

    @Test
    void normalPositiveInterestLoan() {
        EmiCalculationService.EmiCalculationResult result = service.calculate(
                new BigDecimal("500000.00"), new BigDecimal("8.50"), 24);

        assertNotNull(result.emi());
        assertNotNull(result.totalInterest());
        assertNotNull(result.totalPayable());
        // EMI should be positive
        assertTrue(result.emi().compareTo(BigDecimal.ZERO) > 0);
        // Total payable should be principal + interest
        assertEquals(
                result.totalInterest().add(new BigDecimal("500000.00")),
                result.totalPayable(),
                "Total payable must equal principal + total interest"
        );
        // For 500000 at 8.5% for 24 months, EMI should be approximately 22,727.84
        assertTrue(result.emi().compareTo(new BigDecimal("22700.00")) > 0);
        assertTrue(result.emi().compareTo(new BigDecimal("22800.00")) < 0);
    }

    @Test
    void zeroInterestCalculation() {
        EmiCalculationService.EmiCalculationResult result = service.calculate(
                new BigDecimal("100000.00"), BigDecimal.ZERO, 12);

        // Zero interest: EMI = principal / tenure
        assertEquals(new BigDecimal("8333.33"), result.emi(), "EMI for zero interest should be principal/tenure");
        // Total interest should be zero
        assertTrue(result.totalInterest().compareTo(BigDecimal.ZERO) == 0,
                "Total interest should be zero for zero interest rate");
        // Total payable should equal principal
        assertEquals(new BigDecimal("100000.00"), result.totalPayable(), "Total payable should equal principal");
    }

    @Test
    void invalidPrincipalZero() {
        assertThrows(BusinessRuleException.class,
                () -> service.calculate(BigDecimal.ZERO, new BigDecimal("8.50"), 24),
                "Zero principal must be rejected");
    }

    @Test
    void invalidPrincipalNegative() {
        assertThrows(BusinessRuleException.class,
                () -> service.calculate(new BigDecimal("-1000.00"), new BigDecimal("8.50"), 24),
                "Negative principal must be rejected");
    }

    @Test
    void invalidPrincipalTooLarge() {
        assertThrows(BusinessRuleException.class,
                () -> service.calculate(new BigDecimal("100000000.01"), new BigDecimal("8.50"), 24),
                "Principal over ₹10 crore must be rejected");
    }

    @Test
    void invalidInterestRateNegative() {
        assertThrows(BusinessRuleException.class,
                () -> service.calculate(new BigDecimal("100000.00"), new BigDecimal("-1.00"), 24),
                "Negative interest rate must be rejected");
    }

    @Test
    void invalidInterestRateTooHigh() {
        assertThrows(BusinessRuleException.class,
                () -> service.calculate(new BigDecimal("100000.00"), new BigDecimal("100.01"), 24),
                "Interest rate over 100% must be rejected");
    }

    @Test
    void invalidTenureZero() {
        assertThrows(BusinessRuleException.class,
                () -> service.calculate(new BigDecimal("100000.00"), new BigDecimal("8.50"), 0),
                "Zero tenure must be rejected");
    }

    @Test
    void invalidTenureNegative() {
        assertThrows(BusinessRuleException.class,
                () -> service.calculate(new BigDecimal("100000.00"), new BigDecimal("8.50"), -1),
                "Negative tenure must be rejected");
    }

    @Test
    void invalidTenureTooLarge() {
        assertThrows(BusinessRuleException.class,
                () -> service.calculate(new BigDecimal("100000.00"), new BigDecimal("8.50"), 361),
                "Tenure over 360 months must be rejected");
    }

    @Test
    void monetaryRoundingAndConsistency() {
        EmiCalculationService.EmiCalculationResult result = service.calculate(
                new BigDecimal("100000.00"), new BigDecimal("10.00"), 12);

        // totalPayable should equal principal + totalInterest
        assertEquals(
                result.totalInterest().add(new BigDecimal("100000.00")),
                result.totalPayable(),
                "Total payable must equal principal + total interest"
        );
        // All values should be non-null and non-negative
        assertTrue(result.emi().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.totalInterest().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.totalPayable().compareTo(BigDecimal.ZERO) >= 0);
    }
}
