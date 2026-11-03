package com.finedge.repository;

import com.finedge.entity.LoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@code loan_applications}. The only {@code Integer} ID
 * in the schema (auto-increment); every other repository uses
 * {@code String}.
 *
 * <p>Part D3: borrower-scoped query methods enforce ownership by filtering
 * on the borrower relationship ({@code cust_user_id}), so a borrower can
 * only access their own applications. Uses nested property traversal
 * ({@code borrower.userId}) consistent with existing repository conventions
 * like {@code findByOrganization_OrgId}.
 */
@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Integer> {

    Optional<LoanApplication> findByRef(String ref);

    boolean existsByRef(String ref);

    List<LoanApplication> findByStatus(String status);

    List<LoanApplication> findByCustomerEmail(String customerEmail);

    Page<LoanApplication> findByStatus(String status, Pageable pageable);

    /**
     * Finds an application by ID and borrower user ID. Used for ownership
     * enforcement on get/update/delete operations.
     */
    Optional<LoanApplication> findByIdAndBorrower_UserId(Integer id, String borrowerUserId);

    /**
     * Finds an application by ref and borrower user ID. Used for ownership
     * enforcement on get-by-ref operations.
     */
    Optional<LoanApplication> findByRefAndBorrower_UserId(String ref, String borrowerUserId);

    /**
     * Returns all applications for a given borrower user ID. Used for
     * ownership enforcement on list operations.
     */
    Page<LoanApplication> findByBorrower_UserId(String borrowerUserId, Pageable pageable);

    /**
     * Returns all applications for a given borrower user ID and status.
     * Used for ownership enforcement on list operations with a status filter.
     */
    Page<LoanApplication> findByBorrower_UserIdAndStatus(String borrowerUserId, String status, Pageable pageable);

    /**
     * Returns all applications for a given financier key. Used for
     * financier-scoped list operations so a financier sees only their
     * institution's applications.
     */
    Page<LoanApplication> findByFinancierKey(String financierKey, Pageable pageable);

    /**
     * Returns all applications for a given financier key and status.
     * Used for financier-scoped list operations with a status filter.
     */
    Page<LoanApplication> findByFinancierKeyAndStatus(String financierKey, String status, Pageable pageable);

    /**
     * Finds an application by ID and financier key. Used for ownership
     * enforcement on financier get/approve/reject operations.
     */
    Optional<LoanApplication> findByIdAndFinancierKey(Integer id, String financierKey);
}
