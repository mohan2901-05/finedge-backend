package com.finedge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entity for {@code loan_activity_status} (loan stage history).
 *
 * <p>PK {@code pk_las_id VARCHAR(50)} application-assigned. Both FKs are
 * {@code ON DELETE CASCADE ON UPDATE CASCADE}; mapped LAZY, no JPA cascade.
 *
 * <p>Code mapping (from the dump's column comments, senior Q5 — preserved
 * verbatim as {@code String}, not enums):
 * {@code las_activity_id}: {@code BDE}=Applied, {@code PDOC}=Financier
 * Approved, {@code DID}=Disbursed, {@code FINISH}=Finished,
 * {@code REJ}=Rejected. {@code las_status_id}: {@code A}=Applied,
 * {@code C}=Checked/Approved, {@code D}=Disbursed, {@code S}=Finished,
 * {@code R}=Rejected. Any further meaning (e.g. who may write which stage)
 * is unconfirmed — flagged, not guessed.
 */
@Entity
@Table(name = "loan_activity_status")
public class LoanActivityStatus {

    @Id
    @Column(name = "pk_las_id", length = 50, nullable = false, updatable = false)
    private String activityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "las_user_id", nullable = false)
    private UserInfo user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_loan_id", nullable = false)
    private LoanInfo loan;

    @Column(name = "las_activity_id", length = 50, nullable = false)
    private String activityCode;

    @Column(name = "las_status_id", length = 1, nullable = false, columnDefinition = "CHAR(1)")
    private String statusCode;

    @Column(name = "las_updated_on", nullable = false)
    private LocalDateTime updatedOn;

    public LoanActivityStatus() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.updatedOn == null) {
            this.updatedOn = LocalDateTime.now();
        }
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public LoanInfo getLoan() {
        return loan;
    }

    public void setLoan(LoanInfo loan) {
        this.loan = loan;
    }

    public String getActivityCode() {
        return activityCode;
    }

    public void setActivityCode(String activityCode) {
        this.activityCode = activityCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(LocalDateTime updatedOn) {
        this.updatedOn = updatedOn;
    }
}
