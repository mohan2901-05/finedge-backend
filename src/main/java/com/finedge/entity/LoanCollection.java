package com.finedge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity for {@code loan_collection} (cash-in records).
 *
 * <p>PK {@code pk_collection_id VARCHAR(50)} application-assigned. Four FKs:
 * org, loan and payer are {@code ON DELETE CASCADE}; the collecting agent
 * ({@code collected_by_user_id}) is {@code ON DELETE SET NULL} and is
 * therefore mapped {@code optional = true} — deleting an agent nulls the
 * column instead of the row (covered by an M2 test). Mapped LAZY with no
 * JPA cascades (senior Q4).
 *
 * <p>{@code collection_date} defaults to the current date in the dump
 * ({@code DEFAULT (curdate())}); {@code @PrePersist} mirrors that default
 * for explicit inserts. Amounts are persisted as supplied (senior Q7).
 */
@Entity
@Table(name = "loan_collection")
public class LoanCollection {

    @Id
    @Column(name = "pk_collection_id", length = 50, nullable = false, updatable = false)
    private String collectionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_loan_id", nullable = false)
    private LoanInfo loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_user_id", nullable = false)
    private UserInfo user;

    @Column(name = "collection_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal collectionAmount;

    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @Column(name = "payment_mode", length = 20, nullable = false)
    private String paymentMode = "CASH";

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by_user_id")
    private UserInfo collectedBy;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "SUCCESS";

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    @Column(name = "created_user", length = 50)
    private String createdUser;

    @Column(name = "updated_datetime", nullable = false)
    private LocalDateTime updatedDatetime;

    @Column(name = "updated_user", length = 50)
    private String updatedUser;

    public LoanCollection() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdDatetime == null) {
            this.createdDatetime = now;
        }
        if (this.updatedDatetime == null) {
            this.updatedDatetime = now;
        }
        if (this.collectionDate == null) {
            this.collectionDate = LocalDate.now();
        }
        if (this.paymentMode == null) {
            this.paymentMode = "CASH";
        }
        if (this.status == null) {
            this.status = "SUCCESS";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDatetime = LocalDateTime.now();
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public LoanInfo getLoan() {
        return loan;
    }

    public void setLoan(LoanInfo loan) {
        this.loan = loan;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public BigDecimal getCollectionAmount() {
        return collectionAmount;
    }

    public void setCollectionAmount(BigDecimal collectionAmount) {
        this.collectionAmount = collectionAmount;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(LocalDate collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public UserInfo getCollectedBy() {
        return collectedBy;
    }

    public void setCollectedBy(UserInfo collectedBy) {
        this.collectedBy = collectedBy;
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

    public LocalDateTime getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(LocalDateTime createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public String getCreatedUser() {
        return createdUser;
    }

    public void setCreatedUser(String createdUser) {
        this.createdUser = createdUser;
    }

    public LocalDateTime getUpdatedDatetime() {
        return updatedDatetime;
    }

    public void setUpdatedDatetime(LocalDateTime updatedDatetime) {
        this.updatedDatetime = updatedDatetime;
    }

    public String getUpdatedUser() {
        return updatedUser;
    }

    public void setUpdatedUser(String updatedUser) {
        this.updatedUser = updatedUser;
    }
}
