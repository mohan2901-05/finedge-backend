package com.finedge.repository;

import com.finedge.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@code audit_log}. Read-oriented: history rows are
 * inserted by services, never updated (the entity has no update path).
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByOrganization_OrgIdOrderByCreatedDatetimeDesc(String orgId);

    List<AuditLog> findByUser_UserIdOrderByCreatedDatetimeDesc(String userId);

    Page<AuditLog> findByOrganization_OrgIdOrderByCreatedDatetimeDesc(String orgId, Pageable pageable);

    Page<AuditLog> findByUser_UserIdOrderByCreatedDatetimeDesc(String userId, Pageable pageable);
}
