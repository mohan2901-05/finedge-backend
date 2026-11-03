package com.finedge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RepaymentDetailCreateRequest {

    @NotBlank(message = "Loan ID is required")
    private String loanId;

    @NotNull(message = "Installment number is required")
    private Integer installmentNumber;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Opening principal is required")
    private BigDecimal openingPrincipal;

    @NotNull(message = "Installment amount is required")
    private BigDecimal installmentAmount;

    @NotNull(message = "Principal component is required")
    private BigDecimal principal;

    @NotNull(message = "Interest component is required")
    private BigDecimal interest;

    @Size(max = 1, message = "Advance flag must be a single character")
    private String advanceFlag;

    @NotNull(message = "Closing principal is required")
    private BigDecimal closingPrincipal;

    @NotNull(message = "Rate is required")
    private BigDecimal rate;

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Integer installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getOpeningPrincipal() {
        return openingPrincipal;
    }

    public void setOpeningPrincipal(BigDecimal openingPrincipal) {
        this.openingPrincipal = openingPrincipal;
    }

    public BigDecimal getInstallmentAmount() {
        return installmentAmount;
    }

    public void setInstallmentAmount(BigDecimal installmentAmount) {
        this.installmentAmount = installmentAmount;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public BigDecimal getInterest() {
        return interest;
    }

    public void setInterest(BigDecimal interest) {
        this.interest = interest;
    }

    public String getAdvanceFlag() {
        return advanceFlag;
    }

    public void setAdvanceFlag(String advanceFlag) {
        this.advanceFlag = advanceFlag;
    }

    public BigDecimal getClosingPrincipal() {
        return closingPrincipal;
    }

    public void setClosingPrincipal(BigDecimal closingPrincipal) {
        this.closingPrincipal = closingPrincipal;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}
