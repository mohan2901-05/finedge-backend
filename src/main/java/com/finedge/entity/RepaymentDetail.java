package com.finedge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity for {@code repayment_details} (amortisation schedule rows).
 *
 * <p>PK {@code pk_repayment_id VARCHAR(50)} application-assigned.
 * {@code fk_loan_id} is {@code ON DELETE CASCADE ON UPDATE CASCADE}; mapped
 * LAZY, no JPA cascade. All money columns keep their dump precision/scale
 * and are persisted as supplied (senior Q7 — no EMI/principal/interest
 * recalculation here). {@code adv_flag CHAR(1)} stays a {@code String}
 * ({@code Y}/{@code N}).
 */
@Entity
@Table(name = "repayment_details")
public class RepaymentDetail {

    @Id
    @Column(name = "pk_repayment_id", length = 50, nullable = false, updatable = false)
    private String repaymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_loan_id", nullable = false)
    private LoanInfo loan;

    @Column(name = "instl_num", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "opening_principal", precision = 15, scale = 2, nullable = false)
    private BigDecimal openingPrincipal;

    @Column(name = "inst_amt", precision = 15, scale = 2, nullable = false)
    private BigDecimal installmentAmount;

    @Column(name = "principal", precision = 15, scale = 2, nullable = false)
    private BigDecimal principal;

    @Column(name = "interest", precision = 15, scale = 2, nullable = false)
    private BigDecimal interest;

    @Column(name = "adv_flag", length = 1, nullable = false, columnDefinition = "CHAR(1)")
    private String advanceFlag = "N";

    @Column(name = "closing_principal", precision = 15, scale = 2, nullable = false)
    private BigDecimal closingPrincipal;

    @Column(name = "rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal rate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RepaymentDetail() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.advanceFlag == null) {
            this.advanceFlag = "N";
        }
    }

    public String getRepaymentId() {
        return repaymentId;
    }

    public void setRepaymentId(String repaymentId) {
        this.repaymentId = repaymentId;
    }

    public LoanInfo getLoan() {
        return loan;
    }

    public void setLoan(LoanInfo loan) {
        this.loan = loan;
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
