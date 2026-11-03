package com.finedge.repository;

import com.finedge.entity.LoanDisbursement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@code loan_disbursements}.
 */
@Repository
public interface LoanDisbursementRepository extends JpaRepository<LoanDisbursement, String> {

    Optional<LoanDisbursement> findByApplicationId(Integer applicationId);

    boolean existsByApplicationId(Integer applicationId);
}
