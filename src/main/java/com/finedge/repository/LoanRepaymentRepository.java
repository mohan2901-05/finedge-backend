package com.finedge.repository;

import com.finedge.entity.LoanRepayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@code loan_repayments}.
 */
@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, String> {

    List<LoanRepayment> findByApplicationIdOrderByPaidAtDesc(Integer applicationId);

    Page<LoanRepayment> findByApplicationIdOrderByPaidAtDesc(Integer applicationId, Pageable pageable);
}
