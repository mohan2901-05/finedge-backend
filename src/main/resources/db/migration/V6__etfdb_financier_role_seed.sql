-- ========================================================================
-- ETFDB financier role seed (M2 Part C)
-- Adds the FINANCIER_ADMIN role required to authorize financier write
-- operations (create / update / deactivate).
-- Branch: feature/etfdb-m1-iam | Database: etfdb_local (dedicated)
-- ------------------------------------------------------------------------
-- Fidelity notes:
-- * Additive only: does not modify V1-V5.
-- * role_name is 'FINANCIER_ADMIN' (exact case) so the JWT authority
--   'ROLE_' + roleName = 'ROLE_FINANCIER_ADMIN' matches the
--   @PreAuthorize('hasAuthority(...)') expressions on FinancierController.
-- * pk_role_id follows the existing ROLE-T01 / ROLE-T02 convention from V2.
-- * WHERE NOT EXISTS avoids duplicate rows on re-apply.
-- * No user is assigned this role here: role assignment to a specific
--   user_info row is a controlled administrative action (see report).
-- ========================================================================

INSERT INTO role_master (pk_role_id, role_org_id, role_name, role_description, status, created_user, updated_user)
SELECT 'ROLE-T03', 'ORG-T001', 'FINANCIER_ADMIN', 'Financier directory administrator', 'ACTIVE', 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM role_master WHERE pk_role_id = 'ROLE-T03');
