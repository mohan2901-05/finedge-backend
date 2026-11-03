package com.finedge.repository;

import com.finedge.entity.ModuleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * M1 repository for {@code module_master}.
 */
@Repository
public interface ModuleMasterRepository extends JpaRepository<ModuleMaster, String> {

    Optional<ModuleMaster> findByModuleCode(String moduleCode);

    boolean existsByModuleCode(String moduleCode);
}
