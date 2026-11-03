-- ========================================================================
-- ETFDB M1 fabricated seed (NO real PII; for local dev only)
-- Org -> roles -> users -> modules/permissions -> 2 mappings
-- IDs are caller-supplied literals (no generator in M1).
-- ========================================================================

INSERT INTO org_info (pk_org_id, org_name, org_email, status, created_user, updated_user)
VALUES ('ORG-T001', 'Test Org One', 'test-org-one@example.invalid', 'ACTIVE', 'SYSTEM', 'SYSTEM');

INSERT INTO role_master (pk_role_id, role_org_id, role_name, role_description, status, created_user, updated_user)
VALUES
    ('ROLE-T01', 'ORG-T001', 'Financier-Test', 'Test approver role', 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('ROLE-T02', 'ORG-T001', 'Collector-Test', 'Test collector role', 'ACTIVE', 'SYSTEM', 'SYSTEM');

INSERT INTO module_master (pk_module_id, module_name, module_code, created_user, updated_user)
VALUES
    ('MOD-T01', 'Loan-Test', 'LOAN_TEST', 'SYSTEM', 'SYSTEM'),
    ('MOD-T02', 'Collection-Test', 'COLLECTION_TEST', 'SYSTEM', 'SYSTEM');

INSERT INTO permission_master (pk_permission_id, permission_name, permission_code, created_user, updated_user)
VALUES
    ('PERM-T01', 'View-Test', 'VIEW_TEST', 'SYSTEM', 'SYSTEM'),
    ('PERM-T02', 'Approve-Test', 'APPROVE_TEST', 'SYSTEM', 'SYSTEM');

-- Users reference bcrypt-shaped placeholders, NOT real hashes.
INSERT INTO user_info (pk_user_id, user_org_id, user_name, password, user_email, dob, pan_no, user_role_id, status, created_user, updated_user)
VALUES
    ('USR-T001', 'ORG-T001', 'Test Financier', '{bcrypt}PLACEHOLDER-HASH-001', 'financier-test@example.invalid', '1990-01-01', NULL, 'ROLE-T01', 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('USR-T002', 'ORG-T001', 'Test Collector', '{bcrypt}PLACEHOLDER-HASH-002', 'collector-test@example.invalid', '1992-02-02', NULL, 'ROLE-T02', 'ACTIVE', 'SYSTEM', 'SYSTEM');

INSERT INTO role_permission_mapping (pk_mapping_id, map_org_id, map_role_id, map_module_id, map_permission_id, created_user, updated_user)
VALUES
    ('MAP-T001', 'ORG-T001', 'ROLE-T01', 'MOD-T01', 'PERM-T02', 'SYSTEM', 'SYSTEM'),
    ('MAP-T002', 'ORG-T001', 'ROLE-T02', 'MOD-T02', 'PERM-T01', 'SYSTEM', 'SYSTEM');
