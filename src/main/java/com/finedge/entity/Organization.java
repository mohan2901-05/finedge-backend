package com.finedge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * M1 IAM entity for {@code org_info} (tenant root).
 *
 * <p>Source of truth: {@code etfdb_dev.sql} {@code CREATE TABLE org_info} plus
 * its PK/index/FK section. No auto-generated ID: {@code pk_org_id} is
 * application-assigned {@code VARCHAR(50)} (e.g. {@code ORG001} in the dump).
 * No {@code @GeneratedValue} by design (senior Q2: generator proposal pending,
 * so callers supply IDs in M1).
 *
 * <p>Timestamp columns mirror the dump ({@code DATETIME DEFAULT
 * CURRENT_TIMESTAMP} + {@code ON UPDATE CURRENT_TIMESTAMP} on
 * {@code updated_datetime}). The {@code @PrePersist}/{@code @PreUpdate}
 * assignments only fill nulls for portability (H2 + explicit Hibernate
 * inserts); the DDL defaults remain authoritative on MySQL.
 */
@Entity
@Table(name = "org_info")
public class Organization {

    @Id
    @Column(name = "pk_org_id", length = 50, nullable = false, updatable = false)
    private String orgId;

    @Column(name = "org_name", length = 100, nullable = false)
    private String orgName;

    @Column(name = "org_email", length = 50, nullable = false)
    private String orgEmail;

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

    public Organization() {
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

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getOrgEmail() {
        return orgEmail;
    }

    public void setOrgEmail(String orgEmail) {
        this.orgEmail = orgEmail;
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
