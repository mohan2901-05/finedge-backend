-- ========================================================================
-- ETFDB loan application borrower FK correction (M2 Part D4)
-- Replaces ON DELETE CASCADE with ON DELETE RESTRICT on loan_applications
-- Branch: feature/etfdb-m1-iam | Database: etfdb_local (dedicated)
-- ------------------------------------------------------------------------
-- Fidelity notes:
-- * Additive only: does not modify V1-V8.
-- * V8 created fk_loan_app_user_id with ON DELETE CASCADE, which would
--   delete a borrower's loan applications when the user is deleted. That
--   is unacceptable for financial records.
-- * This migration drops and recreates the same constraint with
--   ON DELETE RESTRICT: the database prevents deleting a user while
--   dependent loan applications remain.
-- * The column cust_user_id remains nullable to preserve historical
--   records that have no known borrower (V4 seed id=1).
-- * Constraint name is preserved (fk_loan_app_user_id) for consistency.
-- ========================================================================

-- Drop the ON DELETE CASCADE constraint created by V8.
-- Uses DROP FOREIGN KEY (MySQL-compatible syntax) which H2 also accepts
-- in MySQL compatibility mode.
ALTER TABLE loan_applications DROP FOREIGN KEY fk_loan_app_user_id;

-- Recreate with ON DELETE RESTRICT to protect financial records
ALTER TABLE loan_applications
    ADD CONSTRAINT fk_loan_app_user_id FOREIGN KEY (cust_user_id)
        REFERENCES user_info (pk_user_id) ON DELETE RESTRICT;
