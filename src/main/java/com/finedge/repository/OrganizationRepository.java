package com.finedge.repository;

import com.finedge.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * M1 repository for {@code org_info}. ID type is {@code String}
 * (application-assigned {@code pk_org_id}; no generator in M1).
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findByOrgEmail(String orgEmail);

    boolean existsByOrgEmail(String orgEmail);

    Page<Organization> findByStatus(String status, Pageable pageable);
}
