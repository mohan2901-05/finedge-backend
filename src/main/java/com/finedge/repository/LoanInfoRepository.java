package com.finedge.repository;

import com.finedge.entity.LoanInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@code loan_info}.
 */
@Repository
public interface LoanInfoRepository extends JpaRepository<LoanInfo, String> {

    List<LoanInfo> findByOrganization_OrgId(String orgId);

    List<LoanInfo> findByUser_UserId(String userId);

    List<LoanInfo> findByStatus(String status);

    Page<LoanInfo> findByOrganization_OrgId(String orgId, Pageable pageable);

    Page<LoanInfo> findByUser_UserId(String userId, Pageable pageable);

    Page<LoanInfo> findByStatus(String status, Pageable pageable);
}
