package com.finedge.etfdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.finedge.repository.LoanApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the V8 borrower association migration and V9 FK correction:
 * {@code cust_user_id} column and foreign key are present on
 * {@code loan_applications}. The foreign key uses ON DELETE RESTRICT (not
 * CASCADE) to prevent deleting a user while dependent loan applications
 * remain. Existing records are not assigned to any borrower
 * (NULL cust_user_id).
 *
 * <p>Runs against the default H2 profile with the full Flyway chain
 * (V1-V9) applied automatically by Spring Boot test startup.
 *
 * <p>Note: H2 with {@code DATABASE_TO_LOWER=TRUE} stores all identifiers
 * in lowercase, so schema queries use lowercase names.
 */
@SpringBootTest
@Transactional
class LoanApplicationMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LoanApplicationRepository applicationRepository;

    @Test
    void v8_custUserIdColumnExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = 'loan_applications' AND COLUMN_NAME = 'cust_user_id'",
                Integer.class);
        assertEquals(1, count, "cust_user_id column must exist on loan_applications");
    }

    @Test
    void v8_foreignKeyExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                        "WHERE TABLE_NAME = 'loan_applications' AND CONSTRAINT_NAME = 'fk_loan_app_user_id'",
                Integer.class);
        assertEquals(1, count, "fk_loan_app_user_id foreign key must exist");
    }

    @Test
    void v9_foreignKeyUsesRestrictNotCascade() {
        // Verify the FK constraint uses ON DELETE RESTRICT, not CASCADE.
        // H2 stores the delete rule in INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS.
        String deleteRule = jdbcTemplate.queryForObject(
                "SELECT DELETE_RULE FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS " +
                        "WHERE CONSTRAINT_NAME = 'fk_loan_app_user_id'",
                String.class);
        assertEquals("RESTRICT", deleteRule,
                "Foreign key must use ON DELETE RESTRICT, not CASCADE");
    }

    @Test
    void v8_existingRecordsHaveNullBorrower() {
        // The V4 seed record (id=1) must not be assigned to any borrower.
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM loan_applications WHERE ref = 'TEST-REF-0001' AND cust_user_id IS NULL",
                Integer.class);
        assertEquals(1, count,
                "Existing V4 seed record must have NULL cust_user_id (no arbitrary borrower assigned)");
    }

    @Test
    void v9_loanApplicationContextStarts() {
        // If Flyway V9 fails to apply, the context will not start.
        assertNotNull(applicationRepository.findAll(),
                "loan_applications table must be queryable after V9 migration");
    }
}
