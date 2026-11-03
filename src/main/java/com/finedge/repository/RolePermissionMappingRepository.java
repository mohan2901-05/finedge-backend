package com.finedge.repository;

import com.finedge.entity.RolePermissionMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * M1 repository for {@code role_permission_mapping}. Role-permission
 * retrieval for a role (M3: {@code GET /roles/{id}/permissions}) goes
 * through {@code findByRole_RoleId}; org scoping via
 * {@code findByOrganization_OrgId}.
 */
@Repository
public interface RolePermissionMappingRepository extends JpaRepository<RolePermissionMapping, String> {

    List<RolePermissionMapping> findByRole_RoleId(String roleId);

    List<RolePermissionMapping> findByOrganization_OrgId(String orgId);

    Page<RolePermissionMapping> findByRole_RoleId(String roleId, Pageable pageable);

    Page<RolePermissionMapping> findByOrganization_OrgId(String orgId, Pageable pageable);

    boolean existsByOrganization_OrgIdAndRole_RoleIdAndModule_ModuleIdAndPermission_PermissionId(
            String orgId, String roleId, String moduleId, String permissionId);
}
