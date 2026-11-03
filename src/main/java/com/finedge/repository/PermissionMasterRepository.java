package com.finedge.repository;

import com.finedge.entity.PermissionMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * M1 repository for {@code permission_master}.
 */
@Repository
public interface PermissionMasterRepository extends JpaRepository<PermissionMaster, String> {

    Optional<PermissionMaster> findByPermissionCode(String permissionCode);

    boolean existsByPermissionCode(String permissionCode);
}
