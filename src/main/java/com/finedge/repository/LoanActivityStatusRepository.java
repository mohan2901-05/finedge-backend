package com.finedge.repository;

import com.finedge.entity.LoanActivityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@code loan_activity_status}.
 */
@Repository
public interface LoanActivityStatusRepository extends JpaRepository<LoanActivityStatus, String> {

    List<LoanActivityStatus> findByLoan_LoanId(String loanId);

    List<LoanActivityStatus> findByUser_UserId(String userId);

    List<LoanActivityStatus> findByStatusCode(String statusCode);

    Page<LoanActivityStatus> findByLoan_LoanId(String loanId, Pageable pageable);
}
