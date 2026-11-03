-- ========================================================================
-- ETFDB fabricated loan seed (NO real PII; for local dev only)
-- References V2 IAM seed rows (ORG-T001, USR-T001/USR-T002).
-- Financial figures are illustrative literals, not computed values.
-- ========================================================================

INSERT INTO loan_applications (id, ref, cust_name, cust_phone, cust_email, cust_dob, cust_pan, cust_city, cust_address, financier_key, financier_label, loan_amount, tenure_months, interest_rate, emi, total_interest, total_payable, status)
VALUES (1, 'TEST-REF-0001', 'Test Borrower', '9000000001', 'borrower-test@example.invalid', NULL, NULL, 'Test City', '1 Test Street', 'test_fin', 'Test Finance', 120000.00, 12, 12.00, 10661.85, 7942.20, 127942.20, 'Pending Review');

INSERT INTO loan_info (pk_loan_id, loan_org_id, loan_user_id, loan_amount, interest_rate, tenure_months, purpose, application_date, approval_date, disbursement_date, due_date, outstanding_amount, received, status, created_user, updated_user)
VALUES ('LOAN-T002', 'ORG-T001', 'USR-T001', 120000.00, 12.00, 12, 'Test Finance - Test City', '2026-01-05', '2026-01-06', '2026-01-07', '2027-01-07', 110000.00, 0, 'DISBURSED', 'USR-T001', 'USR-T001');

INSERT INTO loan_activity_status (pk_las_id, las_user_id, fk_loan_id, las_activity_id, las_status_id)
VALUES
    ('LAS-T001', 'USR-T001', 'LOAN-T002', 'BDE', 'A'),
    ('LAS-T002', 'USR-T001', 'LOAN-T002', 'DID', 'D');

INSERT INTO repayment_details (pk_repayment_id, fk_loan_id, instl_num, due_date, opening_principal, inst_amt, principal, interest, adv_flag, closing_principal, rate)
VALUES
    ('REP-T001', 'LOAN-T002', 1, '2026-02-07', 120000.00, 10661.85, 9461.85, 1200.00, 'N', 110538.15, 12.00),
    ('REP-T002', 'LOAN-T002', 2, '2026-03-07', 110538.15, 10661.85, 9556.47, 1105.38, 'N', 100981.68, 12.00),
    ('REP-T003', 'LOAN-T002', 3, '2026-04-07', 100981.68, 10661.85, 9652.03, 1009.82, 'N', 91329.65, 12.00);

INSERT INTO loan_collection (pk_collection_id, collection_org_id, collection_loan_id, collection_user_id, collection_amount, collection_date, payment_mode, reference_no, collected_by_user_id, remarks, status, created_user, updated_user)
VALUES ('COLL-T002', 'ORG-T001', 'LOAN-T002', 'USR-T001', 10661.85, '2026-02-07', 'CASH', 'TEST-REF-001', 'USR-T002', 'Fabricated test collection', 'SUCCESS', 'USR-T002', 'USR-T002');

INSERT INTO audit_log (pk_audit_id, org_id, user_id, user_name, action, details)
VALUES
    ('AUD-T001', 'ORG-T001', 'USR-T001', 'Test Financier', 'DISBURSE_LOAN', 'Fabricated disbursal of LOAN-T002'),
    ('AUD-T002', 'ORG-T001', 'USR-T002', 'Test Collector', 'COLLECT', 'Fabricated collection COLL-T002');
