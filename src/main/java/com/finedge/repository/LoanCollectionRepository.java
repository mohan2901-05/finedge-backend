package com.finedge.repository;

import com.finedge.entity.LoanCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@code loan_collection}.
 */
@Repository
public interface LoanCollectionRepository extends JpaRepository<LoanCollection, String> {

    List<LoanCollection> findByLoan_LoanId(String loanId);

    List<LoanCollection> findByOrganization_OrgId(String orgId);

    List<LoanCollection> findByUser_UserId(String userId);

    List<LoanCollection> findByStatus(String status);

    Page<LoanCollection> findByLoan_LoanId(String loanId, Pageable pageable);

    Page<LoanCollection> findByOrganization_OrgId(String orgId, Pageable pageable);

    Page<LoanCollection> findByStatus(String status, Pageable pageable);
}
