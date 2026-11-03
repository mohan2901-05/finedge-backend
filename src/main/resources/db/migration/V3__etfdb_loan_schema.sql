-- ========================================================================
-- ETFDB loan schema (derived from etfdb_dev.sql source of truth)
-- Tables: loan_applications, loan_info, loan_activity_status,
--         repayment_details, loan_collection, audit_log
-- Branch: feature/etfdb-m1-iam | Database: etfdb_local (dedicated)
-- ------------------------------------------------------------------------
-- Fidelity notes (per senior Q3/Q4/Q5/Q7/Q8):
-- * PKs, UNIQUEs, KEYs, FKs and ON DELETE/UPDATE rules preserved exactly.
-- * ENGINE=InnoDB / CHARSET/COLLATE / SET / phpMyAdmin headers omitted.
-- * loan_applications is the only AUTO_INCREMENT table and declares NO
--   foreign keys -- no intake->loan relationship is invented.
-- * Financial columns keep DECIMAL(15,2)/DECIMAL(5,2); values are stored
--   as supplied, never recalculated by the schema.
-- * CHAR(1) codes (las_status_id, adv_flag) and TINYINT(1) (received)
--   preserved with dump defaults.
-- * DATE DEFAULT (curdate()) and DATETIME DEFAULT CURRENT_TIMESTAMP /
--   ON UPDATE CURRENT_TIMESTAMP preserved (MySQL primary; H2 MySQL-mode
--   compatibility is verified by the M2 test run).
-- ========================================================================

-- 1. loan_applications (standalone intake; no FKs by design)
CREATE TABLE IF NOT EXISTS loan_applications (
    id INT NOT NULL AUTO_INCREMENT,
    ref VARCHAR(50) NOT NULL,
    cust_name VARCHAR(100) NOT NULL,
    cust_phone VARCHAR(15) NOT NULL,
    cust_email VARCHAR(100) NOT NULL,
    cust_dob DATE DEFAULT NULL,
    cust_pan VARCHAR(20) DEFAULT NULL,
    cust_city VARCHAR(100) NOT NULL,
    cust_address VARCHAR(255) NOT NULL,
    financier_key VARCHAR(50) NOT NULL,
    financier_label VARCHAR(100) NOT NULL,
    loan_amount DECIMAL(15,2) NOT NULL,
    tenure_months INT NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    emi DECIMAL(15,2) NOT NULL,
    total_interest DECIMAL(15,2) NOT NULL,
    total_payable DECIMAL(15,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Pending Review',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_loan_applications PRIMARY KEY (id),
    CONSTRAINT uk_ref UNIQUE (ref)
);

-- 2. loan_info (booked loans)
CREATE TABLE IF NOT EXISTS loan_info (
    pk_loan_id VARCHAR(50) NOT NULL,
    loan_org_id VARCHAR(50) NOT NULL,
    loan_user_id VARCHAR(50) NOT NULL,
    loan_amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    tenure_months INT NOT NULL,
    purpose VARCHAR(255) DEFAULT NULL,
    application_date DATE NOT NULL DEFAULT (curdate()),
    approval_date DATE DEFAULT NULL,
    disbursement_date DATE DEFAULT NULL,
    due_date DATE DEFAULT NULL,
    outstanding_amount DECIMAL(15,2) DEFAULT NULL,
    received TINYINT(1) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(50) DEFAULT NULL,
    updated_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_user VARCHAR(50) DEFAULT NULL,
    CONSTRAINT pk_loan_info PRIMARY KEY (pk_loan_id),
    CONSTRAINT fk_loan_org_id FOREIGN KEY (loan_org_id)
        REFERENCES org_info (pk_org_id) ON DELETE CASCADE,
    CONSTRAINT fk_loan_user_id FOREIGN KEY (loan_user_id)
        REFERENCES user_info (pk_user_id) ON DELETE CASCADE
);
CREATE INDEX fk_loan_org_id ON loan_info (loan_org_id);
CREATE INDEX fk_loan_user_id ON loan_info (loan_user_id);
CREATE INDEX idx_loan_status ON loan_info (status);

-- 3. loan_activity_status (stage history; codes per dump comments)
CREATE TABLE IF NOT EXISTS loan_activity_status (
    pk_las_id VARCHAR(50) NOT NULL,
    las_user_id VARCHAR(50) NOT NULL,
    fk_loan_id VARCHAR(50) NOT NULL,
    las_activity_id VARCHAR(50) NOT NULL,
    las_status_id CHAR(1) NOT NULL,
    las_updated_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_loan_activity_status PRIMARY KEY (pk_las_id),
    CONSTRAINT fk_las_loan_id FOREIGN KEY (fk_loan_id)
        REFERENCES loan_info (pk_loan_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_las_user_id FOREIGN KEY (las_user_id)
        REFERENCES user_info (pk_user_id) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX idx_las_loan ON loan_activity_status (fk_loan_id);
CREATE INDEX idx_las_user ON loan_activity_status (las_user_id);
CREATE INDEX idx_las_status ON loan_activity_status (las_status_id);

-- 4. repayment_details (amortisation schedule)
CREATE TABLE IF NOT EXISTS repayment_details (
    pk_repayment_id VARCHAR(50) NOT NULL,
    fk_loan_id VARCHAR(50) NOT NULL,
    instl_num INT NOT NULL,
    due_date DATE NOT NULL,
    opening_principal DECIMAL(15,2) NOT NULL,
    inst_amt DECIMAL(15,2) NOT NULL,
    principal DECIMAL(15,2) NOT NULL,
    interest DECIMAL(15,2) NOT NULL,
    adv_flag CHAR(1) NOT NULL DEFAULT 'N',
    closing_principal DECIMAL(15,2) NOT NULL,
    rate DECIMAL(5,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_repayment_details PRIMARY KEY (pk_repayment_id),
    CONSTRAINT fk_rep_loan_id FOREIGN KEY (fk_loan_id)
        REFERENCES loan_info (pk_loan_id) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX idx_rep_loan ON repayment_details (fk_loan_id);
CREATE INDEX idx_rep_due ON repayment_details (due_date);

-- 5. loan_collection (cash-in; collector FK is SET NULL by design)
CREATE TABLE IF NOT EXISTS loan_collection (
    pk_collection_id VARCHAR(50) NOT NULL,
    collection_org_id VARCHAR(50) NOT NULL,
    collection_loan_id VARCHAR(50) NOT NULL,
    collection_user_id VARCHAR(50) NOT NULL,
    collection_amount DECIMAL(15,2) NOT NULL,
    collection_date DATE NOT NULL DEFAULT (curdate()),
    payment_mode VARCHAR(20) NOT NULL DEFAULT 'CASH',
    reference_no VARCHAR(100) DEFAULT NULL,
    collected_by_user_id VARCHAR(50) DEFAULT NULL,
    remarks VARCHAR(255) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(50) DEFAULT NULL,
    updated_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_user VARCHAR(50) DEFAULT NULL,
    CONSTRAINT pk_loan_collection PRIMARY KEY (pk_collection_id),
    CONSTRAINT fk_collection_org_id FOREIGN KEY (collection_org_id)
        REFERENCES org_info (pk_org_id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_loan_id FOREIGN KEY (collection_loan_id)
        REFERENCES loan_info (pk_loan_id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_user_id FOREIGN KEY (collection_user_id)
        REFERENCES user_info (pk_user_id) ON DELETE CASCADE,
    CONSTRAINT fk_collected_by_user_id FOREIGN KEY (collected_by_user_id)
        REFERENCES user_info (pk_user_id) ON DELETE SET NULL
);
CREATE INDEX fk_collection_org_id ON loan_collection (collection_org_id);
CREATE INDEX fk_collection_loan_id ON loan_collection (collection_loan_id);
CREATE INDEX fk_collection_user_id ON loan_collection (collection_user_id);
CREATE INDEX fk_collected_by_user_id ON loan_collection (collected_by_user_id);
CREATE INDEX idx_collection_status ON loan_collection (status);
CREATE INDEX idx_collection_date ON loan_collection (collection_date);

-- 6. audit_log (append-only trail)
CREATE TABLE IF NOT EXISTS audit_log (
    pk_audit_id VARCHAR(50) NOT NULL,
    org_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    user_name VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    details VARCHAR(500) DEFAULT NULL,
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_log PRIMARY KEY (pk_audit_id),
    CONSTRAINT fk_audit_org_id FOREIGN KEY (org_id)
        REFERENCES org_info (pk_org_id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_user_id FOREIGN KEY (user_id)
        REFERENCES user_info (pk_user_id) ON DELETE CASCADE
);
CREATE INDEX fk_audit_org_id ON audit_log (org_id);
CREATE INDEX fk_audit_user_id ON audit_log (user_id);
CREATE INDEX idx_audit_datetime ON audit_log (created_datetime);
