-- ========================================================================
-- ETFDB user-financier association (M2 Part F1)
-- Links a user_info row to the financier institution they represent.
-- Branch: feature/etfdb-m1-iam | Database: etfdb_local (dedicated)
-- ------------------------------------------------------------------------
-- Fidelity notes:
-- * Additive only: does not modify V1-V9.
-- * Adds nullable financier_id to user_info, FK to financiers.pk_financier_id.
-- * Column is NULLABLE: users not associated with any financier (e.g.
--   borrowers, staff without financier scope) have NULL. No arbitrary
--   association is invented.
-- * ON DELETE SET NULL: deactivating/deleting a financier does not orphan
--   users; they simply lose financier scope.
-- ========================================================================

ALTER TABLE user_info
    ADD COLUMN financier_id VARCHAR(50);

ALTER TABLE user_info
    ADD CONSTRAINT fk_user_financier_id FOREIGN KEY (financier_id)
        REFERENCES financiers (pk_financier_id) ON DELETE SET NULL;

CREATE INDEX fk_user_financier_id ON user_info (financier_id);
