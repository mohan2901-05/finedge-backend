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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * M1 IAM entity for {@code role_permission_mapping} (explicit RBAC join).
 *
 * <p>PK {@code pk_mapping_id VARCHAR(50)} application-assigned. The composite
 * {@code UNIQUE(map_org_id, map_role_id, map_module_id, map_permission_id)}
 * ({@code uk_tenant_security}) is declared here so Hibernate validation sees
 * the same constraint the migration creates. All four FKs are
 * {@code ON DELETE CASCADE}; mapped LAZY with no JPA cascade (senior Q4).
 * A dedicated entity (not {@code @ManyToMany}) is required because the join
 * carries its own PK plus audit columns.
 */
@Entity
@Table(name = "role_permission_mapping",
        uniqueConstraints = @UniqueConstraint(name = "uk_tenant_security",
                columnNames = {"map_org_id", "map_role_id", "map_module_id", "map_permission_id"}))
public class RolePermissionMapping {

    @Id
    @Column(name = "pk_mapping_id", length = 50, nullable = false, updatable = false)
    private String mappingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_role_id", nullable = false)
    private RoleMaster role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_module_id", nullable = false)
    private ModuleMaster module;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_permission_id", nullable = false)
    private PermissionMaster permission;

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    @Column(name = "created_user", length = 50)
    private String createdUser;

    @Column(name = "updated_datetime", nullable = false)
    private LocalDateTime updatedDatetime;

    @Column(name = "updated_user", length = 50)
    private String updatedUser;

    public RolePermissionMapping() {
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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDatetime = LocalDateTime.now();
    }

    public String getMappingId() {
        return mappingId;
    }

    public void setMappingId(String mappingId) {
        this.mappingId = mappingId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public RoleMaster getRole() {
        return role;
    }

    public void setRole(RoleMaster role) {
        this.role = role;
    }

    public ModuleMaster getModule() {
        return module;
    }

    public void setModule(ModuleMaster module) {
        this.module = module;
    }

    public PermissionMaster getPermission() {
        return permission;
    }

    public void setPermission(PermissionMaster permission) {
        this.permission = permission;
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
