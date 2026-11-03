package com.finedge.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Loan update. Only operational fields are mutable here: purpose, lifecycle
 * dates, outstanding amount, received flag, and status. The booked terms
 * (amount, rate, tenure) are immutable after creation — changing them would
 * silently invalidate the repayment schedule, and no recalculation rule has
 * been confirmed. Corrections to terms require deleting and re-booking the
 * loan (which cascades to its schedule, per the schema).
 */
public class LoanInfoUpdateRequest {

    @Size(max = 255, message = "Purpose must be at most 255 characters")
    private String purpose;

    private LocalDate approvalDate;

    private LocalDate disbursementDate;

    private LocalDate dueDate;

    private BigDecimal outstandingAmount;

    private Boolean received;

    @Size(max = 20, message = "Status must be at most 20 characters")
    private String status;

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public LocalDate getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDate approvalDate) {
        this.approvalDate = approvalDate;
    }

    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }

    public void setDisbursementDate(LocalDate disbursementDate) {
        this.disbursementDate = disbursementDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public Boolean getReceived() {
        return received;
    }

    public void setReceived(Boolean received) {
        this.received = received;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
