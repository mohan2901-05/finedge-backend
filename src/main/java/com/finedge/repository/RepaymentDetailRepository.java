package com.finedge.repository;

import com.finedge.entity.RepaymentDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@code repayment_details}. Installment rows are ordered
 * by the caller (e.g. {@code OrderByInstallmentNumberAsc}) since schedule
 * order matters.
 */
@Repository
public interface RepaymentDetailRepository extends JpaRepository<RepaymentDetail, String> {

    List<RepaymentDetail> findByLoan_LoanIdOrderByInstallmentNumberAsc(String loanId);

    Page<RepaymentDetail> findByLoan_LoanIdOrderByInstallmentNumberAsc(String loanId, Pageable pageable);
}
