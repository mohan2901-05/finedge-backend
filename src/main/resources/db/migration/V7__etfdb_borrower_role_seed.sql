-- ========================================================================
-- ETFDB borrower role seed (M2 Part D2)
-- Adds the BORROWER role required for self-service borrower registration.
-- Branch: feature/etfdb-m1-iam | Database: etfdb_local (dedicated)
-- ------------------------------------------------------------------------
-- Fidelity notes:
-- * Additive only: does not modify V1-V6.
-- * role_name is 'BORROWER' (exact case) so the JWT authority
--   'ROLE_' + roleName = 'ROLE_BORROWER' is available for borrower
--   authorization in later phases.
-- * pk_role_id follows the existing ROLE-T01 / ROLE-T02 / ROLE-T03
--   convention from V2 and V6.
-- * role_org_id references ORG-T001 seeded in V2.
-- * WHERE NOT EXISTS avoids duplicate rows on re-apply.
-- * No user is assigned this role here: registration assigns the role
--   per-request via AuthService using the configured default role id.
-- * Does not assign FINANCIER_ADMIN or any administrative role.
-- ========================================================================

INSERT INTO role_master (pk_role_id, role_org_id, role_name, role_description, status, created_user, updated_user)
SELECT 'ROLE-T04', 'ORG-T001', 'BORROWER', 'Borrower (self-service registration role)', 'ACTIVE', 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM role_master WHERE pk_role_id = 'ROLE-T04');
