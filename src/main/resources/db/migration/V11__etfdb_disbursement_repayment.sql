-- ========================================================================
-- ETFDB disbursement and repayment (Roadmap G)
-- Tables: loan_disbursements, loan_repayments
-- Branch: feature/etfdb-m1-iam | Database: etfdb_local (dedicated)
-- ------------------------------------------------------------------------
-- Fidelity notes:
-- * Additive only: does not modify V1-V10.
-- * loan_disbursements records a single disbursement per loan application
--   (one-to-one enforced by UNIQUE(fk_loan_application_id)); prevents
--   duplicate disbursement at the database level.
-- * loan_repayments records each repayment as an individual row against a
--   disbursed loan application; outstanding balance is computed from the
--   persisted disbursement and repayment rows (no stored balance column).
-- * All money columns are DECIMAL(15,2); values are stored as supplied
--   (rounded to 2 decimals, HALF_UP) by the service layer.
-- * IDs are application-assigned VARCHAR(50) via StringIdGenerator.
-- * FKs reference loan_applications.id (INT auto-increment) and user_info.
-- ========================================================================

CREATE TABLE IF NOT EXISTS loan_disbursements (
    pk_disbursement_id VARCHAR(50) NOT NULL,
    fk_loan_application_id INT NOT NULL,
    disbursed_amount DECIMAL(15,2) NOT NULL,
    disbursed_at DATETIME NOT NULL,
    disbursed_by_user_id VARCHAR(50) NOT NULL,
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_loan_disbursements PRIMARY KEY (pk_disbursement_id),
    CONSTRAINT fk_disbursement_loan_application FOREIGN KEY (fk_loan_application_id)
        REFERENCES loan_applications (id) ON DELETE RESTRICT,
    CONSTRAINT fk_disbursement_user FOREIGN KEY (disbursed_by_user_id)
        REFERENCES user_info (pk_user_id) ON DELETE RESTRICT,
    CONSTRAINT uk_disbursement_one_per_application UNIQUE (fk_loan_application_id)
);
CREATE INDEX fk_disbursement_loan_application ON loan_disbursements (fk_loan_application_id);
CREATE INDEX fk_disbursement_user ON loan_disbursements (disbursed_by_user_id);

CREATE TABLE IF NOT EXISTS loan_repayments (
    pk_repayment_id VARCHAR(50) NOT NULL,
    fk_loan_application_id INT NOT NULL,
    repayment_amount DECIMAL(15,2) NOT NULL,
    paid_at DATETIME NOT NULL,
    paid_by_user_id VARCHAR(50) NOT NULL,
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_loan_repayments PRIMARY KEY (pk_repayment_id),
    CONSTRAINT fk_repayment_loan_application FOREIGN KEY (fk_loan_application_id)
        REFERENCES loan_applications (id) ON DELETE RESTRICT,
    CONSTRAINT fk_repayment_user FOREIGN KEY (paid_by_user_id)
        REFERENCES user_info (pk_user_id) ON DELETE RESTRICT
);
CREATE INDEX fk_repayment_loan_application ON loan_repayments (fk_loan_application_id);
CREATE INDEX fk_repayment_user ON loan_repayments (paid_by_user_id);
CREATE INDEX idx_repayment_paid_at ON loan_repayments (paid_at);
