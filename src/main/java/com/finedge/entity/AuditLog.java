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
 * Entity for {@code audit_log} (append-only trail).
 *
 * <p>PK {@code pk_audit_id VARCHAR(50)} application-assigned. Both FKs are
 * {@code ON DELETE CASCADE}; mapped LAZY, no JPA cascade. {@code user_name}
 * is a denormalised actor snapshot stored alongside {@code user_id} in the
 * dump — kept as a plain column, not derived.
 *
 * <p>History-oriented: no update/delete service behavior is planned for
 * this table (writes are inserts only; deletes happen solely through the
 * dump's CASCADE rules). There is intentionally no {@code @PreUpdate}.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @Column(name = "pk_audit_id", length = 50, nullable = false, updatable = false)
    private String auditId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserInfo user;

    @Column(name = "user_name", length = 50, nullable = false)
    private String userName;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "details", length = 500)
    private String details;

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    public AuditLog() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdDatetime == null) {
            this.createdDatetime = LocalDateTime.now();
        }
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(LocalDateTime createdDatetime) {
        this.createdDatetime = createdDatetime;
    }
}
