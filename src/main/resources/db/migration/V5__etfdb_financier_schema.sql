-- ========================================================================
-- ETFDB financier schema (M2 Part C)
-- Table: financiers (lender directory)
-- Branch: feature/etfdb-m1-iam | Database: etfdb_local (dedicated)
-- ------------------------------------------------------------------------
-- Fidelity notes:
-- * PKs, UNIQUEs, KEYs preserved exactly.
-- * ENGINE=InnoDB / CHARSET/COLLATE omitted: MySQL defaults apply.
-- * DATETIME DEFAULT CURRENT_TIMESTAMP + ON UPDATE CURRENT_TIMESTAMP
--   preserved (MySQL primary; H2 MySQL-mode compatibility follows existing
--   convention in V1/V3).
-- * IDs are application-assigned VARCHAR(50); no generator in the schema
--   (callers supply IDs via StringIdGenerator).
-- * No seed data inserted here: do not fabricate financial institutions.
-- ========================================================================

CREATE TABLE IF NOT EXISTS financiers (
    pk_financier_id VARCHAR(50) NOT NULL,
    institution_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    indicative_annual_rate DECIMAL(5,2) DEFAULT NULL,
    min_loan_amount DECIMAL(15,2) DEFAULT NULL,
    max_loan_amount DECIMAL(15,2) DEFAULT NULL,
    available_tenure_months INT DEFAULT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(50) DEFAULT NULL,
    updated_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_user VARCHAR(50) DEFAULT NULL,
    CONSTRAINT pk_financiers PRIMARY KEY (pk_financier_id),
    CONSTRAINT uk_financier_institution UNIQUE (institution_name)
);
CREATE INDEX idx_financiers_status ON financiers (status);
CREATE INDEX idx_financiers_institution ON financiers (institution_name);
