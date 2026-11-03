package com.finedge.repository;

import com.finedge.entity.Financier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * M2 Part C repository for {@code financiers}. ID type is {@code String}
 * (application-assigned {@code pk_financier_id}; no generator in the
 * schema).
 */
@Repository
public interface FinancierRepository extends JpaRepository<Financier, String> {

    Optional<Financier> findByInstitutionName(String institutionName);

    boolean existsByInstitutionName(String institutionName);

    Page<Financier> findByStatus(String status, Pageable pageable);

    Page<Financier> findByInstitutionNameContainingIgnoreCase(
            String institutionName, Pageable pageable);
}
