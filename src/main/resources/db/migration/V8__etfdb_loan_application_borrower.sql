-- ========================================================================
-- ETFDB loan application borrower association (M2 Part D3)
-- Associates loan_applications with the authenticated borrower (user_info).
-- Branch: feature/etfdb-m1-iam | Database: etfdb_local (dedicated)
-- ------------------------------------------------------------------------
-- Fidelity notes:
-- * Additive only: does not modify V1-V7.
-- * Adds cust_user_id to loan_applications, referencing user_info.pk_user_id.
-- * Column is NULLABLE: existing records (including V4 seed id=1) are not
--   assigned to any borrower. No arbitrary ownership is invented.
-- * FK uses ON DELETE CASCADE consistent with other user_info foreign keys
--   in the dump (loan_info.fk_loan_user_id, loan_collection, audit_log, etc.).
-- * Index follows existing fk_* naming convention.
-- ========================================================================

ALTER TABLE loan_applications
    ADD COLUMN cust_user_id VARCHAR(50);

ALTER TABLE loan_applications
    ADD CONSTRAINT fk_loan_app_user_id FOREIGN KEY (cust_user_id)
        REFERENCES user_info (pk_user_id) ON DELETE CASCADE;

-- H2 automatically creates an index for the foreign key column; no separate
-- CREATE INDEX is needed (and would conflict with the auto-generated index).

CREATE INDEX fk_loan_app_user_id ON loan_applications (cust_user_id);
