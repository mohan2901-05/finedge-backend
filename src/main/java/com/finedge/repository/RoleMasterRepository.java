package com.finedge.repository;

import com.finedge.entity.RoleMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * M1 repository for {@code role_master}. Includes org-scoped finder because
 * roles belong to an organization ({@code role_org_id}).
 */
@Repository
public interface RoleMasterRepository extends JpaRepository<RoleMaster, String> {

    List<RoleMaster> findByOrganization_OrgId(String orgId);

    List<RoleMaster> findByOrganization_OrgIdAndStatus(String orgId, String status);

    Page<RoleMaster> findByOrganization_OrgId(String orgId, Pageable pageable);
}
