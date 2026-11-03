package com.finedge.repository;

import com.finedge.entity.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * M1 repository for {@code user_info}. Covers the unique-email lookup and
 * org/role-scoped queries needed by later RBAC work. No password-based
 * query is exposed (hashes never leave the service layer).
 */
@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, String> {

    Optional<UserInfo> findByUserEmail(String userEmail);

    boolean existsByUserEmail(String userEmail);

    List<UserInfo> findByOrganization_OrgId(String orgId);

    List<UserInfo> findByRole_RoleId(String roleId);

    Page<UserInfo> findByOrganization_OrgId(String orgId, Pageable pageable);

    Page<UserInfo> findByRole_RoleId(String roleId, Pageable pageable);
}
