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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * M1 IAM entity for {@code user_info} (staff + customer users).
 *
 * <p>PK {@code pk_user_id VARCHAR(50)} application-assigned. {@code user_email}
 * is unique. FKs {@code user_org_id} and {@code user_role_id} are both
 * {@code ON DELETE CASCADE} in the dump; mapped LAZY with no JPA cascade.
 *
 * <p>Security note (senior Q6): {@code password} holds a hash and
 * {@code pan_no} holds a sensitive identifier. Both are mapped because the
 * schema requires them, but they must never be exposed in API responses —
 * M3 DTOs will exclude them. No authentication/JWT is implemented in M1.
 */
@Entity
@Table(name = "user_info")
public class UserInfo {

    @Id
    @Column(name = "pk_user_id", length = 50, nullable = false, updatable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_org_id", nullable = false)
    private Organization organization;

    @Column(name = "user_name", length = 50, nullable = false)
    private String userName;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "user_email", length = 50, nullable = false, unique = true)
    private String userEmail;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "pan_no", length = 20)
    private String panNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_role_id", nullable = false)
    private RoleMaster role;

    /**
     * Optional association to the financier institution this user represents.
     * Nullable: borrowers and non-financier staff have no financier scope.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "financier_id", nullable = true)
    private Financier financier;

    @Column(name = "status", length = 10, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    @Column(name = "created_user", length = 50)
    private String createdUser;

    @Column(name = "updated_datetime", nullable = false)
    private LocalDateTime updatedDatetime;

    @Column(name = "updated_user", length = 50)
    private String updatedUser;

    public UserInfo() {
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
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDatetime = LocalDateTime.now();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getPanNo() {
        return panNo;
    }

    public void setPanNo(String panNo) {
        this.panNo = panNo;
    }

    public RoleMaster getRole() {
        return role;
    }

    public void setRole(RoleMaster role) {
        this.role = role;
    }

    public Financier getFinancier() {
        return financier;
    }

    public void setFinancier(Financier financier) {
        this.financier = financier;
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
