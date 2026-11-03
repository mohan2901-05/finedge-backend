package com.finedge.dto;

import jakarta.validation.constraints.Size;

/**
 * Collection update. Only administrative annotations are mutable: remarks,
 * status, and reference number. Amount, date, parties, and payment mode
 * are immutable after posting — changing them would rewrite ledger
 * history, and no correction workflow has been confirmed.
 */
public class LoanCollectionUpdateRequest {

    @Size(max = 100, message = "Reference number must be at most 100 characters")
    private String referenceNo;

    @Size(max = 255, message = "Remarks must be at most 255 characters")
    private String remarks;

    @Size(max = 20, message = "Status must be at most 20 characters")
    private String status;

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
