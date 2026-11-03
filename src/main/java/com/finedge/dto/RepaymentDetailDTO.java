package com.finedge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RepaymentDetailDTO {
    private String repaymentId;
    private String loanId;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal openingPrincipal;
    private BigDecimal installmentAmount;
    private BigDecimal principal;
    private BigDecimal interest;
    private String advanceFlag;
    private BigDecimal closingPrincipal;
    private BigDecimal rate;
    private LocalDateTime createdAt;

    public RepaymentDetailDTO() {
    }

    public String getRepaymentId() {
        return repaymentId;
    }

    public void setRepaymentId(String repaymentId) {
        this.repaymentId = repaymentId;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
