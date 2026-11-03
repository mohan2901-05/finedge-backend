-- ========================================================================
-- ETFDB M1 IAM Schema (derived from etfdb_dev.sql source of truth)
-- Tables: org_info, role_master, user_info, module_master,
--         permission_master, role_permission_mapping
-- Branch: feature/etfdb-m1-iam | Database: etfdb_local (dedicated)
-- ------------------------------------------------------------------------
-- Fidelity notes (per senior Q4/Q8):
-- * PKs, UNIQUEs, KEYs, FKs and ON DELETE/UPDATE rules preserved exactly.
-- * ENGINE=InnoDB / CHARSET/COLLATE / SET / phpMyAdmin headers omitted:
--   MySQL defaults apply; behavior unchanged for InnoDB/utf8mb4 setups.
-- * DATETIME DEFAULT CURRENT_TIMESTAMP + ON UPDATE CURRENT_TIMESTAMP
--   preserved (MySQL primary). H2 MySQL-mode may reject ON UPDATE;
--   MySQL parity run is still required (see M1 report).
-- * No AUTO_INCREMENT in M1 scope (only loan_applications has it, M2).
-- * IDs are application-assigned VARCHAR(50); no generator implemented
--   in M1 (awaiting approval of proposed format).
-- ========================================================================

-- 1. org_info (tenant root)
CREATE TABLE IF NOT EXISTS org_info (
    pk_org_id VARCHAR(50) NOT NULL,
    org_name VARCHAR(100) NOT NULL,
    org_email VARCHAR(50) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(50) DEFAULT NULL,
    updated_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_user VARCHAR(50) DEFAULT NULL,
    CONSTRAINT pk_org_info PRIMARY KEY (pk_org_id)
);

-- 2. role_master (per-org roles)
CREATE TABLE IF NOT EXISTS role_master (
    pk_role_id VARCHAR(50) NOT NULL,
    role_org_id VARCHAR(50) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    role_description VARCHAR(255) DEFAULT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(50) DEFAULT NULL,
    updated_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_user VARCHAR(50) DEFAULT NULL,
    CONSTRAINT pk_role_master PRIMARY KEY (pk_role_id),
    CONSTRAINT fk_role_org_id FOREIGN KEY (role_org_id)
        REFERENCES org_info (pk_org_id) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX fk_role_org_id ON role_master (role_org_id);

-- 3. user_info (staff + customers; password/PAN opaque, never exposed via API)
CREATE TABLE IF NOT EXISTS user_info (
    pk_user_id VARCHAR(50) NOT NULL,
    user_org_id VARCHAR(50) NOT NULL,
    user_name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    user_email VARCHAR(50) NOT NULL,
    dob DATE DEFAULT NULL,
    pan_no VARCHAR(20) DEFAULT NULL,
    user_role_id VARCHAR(50) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(50) DEFAULT NULL,
    updated_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_user VARCHAR(50) DEFAULT NULL,
    CONSTRAINT pk_user_info PRIMARY KEY (pk_user_id),
    CONSTRAINT user_email UNIQUE (user_email),
    CONSTRAINT fk_user_org_id FOREIGN KEY (user_org_id)
        REFERENCES org_info (pk_org_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_id FOREIGN KEY (user_role_id)
        REFERENCES role_master (pk_role_id) ON DELETE CASCADE
);
CREATE INDEX fk_user_org_id ON user_info (user_org_id);
CREATE INDEX fk_user_role_id ON user_info (user_role_id);

-- 4. module_master
CREATE TABLE IF NOT EXISTS module_master (
    pk_module_id VARCHAR(50) NOT NULL,
    module_name VARCHAR(50) NOT NULL,
    module_code VARCHAR(50) NOT NULL,
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(50) DEFAULT NULL,
    updated_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_user VARCHAR(50) DEFAULT NULL,
    CONSTRAINT pk_module_master PRIMARY KEY (pk_module_id),
    CONSTRAINT module_code UNIQUE (module_code)
);

-- 5. permission_master
CREATE TABLE IF NOT EXISTS permission_master (
    pk_permission_id VARCHAR(50) NOT NULL,
    permission_name VARCHAR(50) NOT NULL,
    permission_code VARCHAR(50) NOT NULL,
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(50) DEFAULT NULL,
    updated_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_user VARCHAR(50) DEFAULT NULL,
    CONSTRAINT pk_permission_master PRIMARY KEY (pk_permission_id),
    CONSTRAINT permission_code UNIQUE (permission_code)
);

-- 6. role_permission_mapping (explicit RBAC join; no @ManyToMany in JPA)
CREATE TABLE IF NOT EXISTS role_permission_mapping (
    pk_mapping_id VARCHAR(50) NOT NULL,
    map_org_id VARCHAR(50) NOT NULL,
    map_role_id VARCHAR(50) NOT NULL,
    map_module_id VARCHAR(50) NOT NULL,
    map_permission_id VARCHAR(50) NOT NULL,
    created_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_user VARCHAR(50) DEFAULT NULL,
    updated_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_user VARCHAR(50) DEFAULT NULL,
    CONSTRAINT pk_role_permission_mapping PRIMARY KEY (pk_mapping_id),
    CONSTRAINT uk_tenant_security UNIQUE (map_org_id, map_role_id, map_module_id, map_permission_id),
    CONSTRAINT fk_map_org_id FOREIGN KEY (map_org_id)
        REFERENCES org_info (pk_org_id) ON DELETE CASCADE,
    CONSTRAINT fk_map_role_id FOREIGN KEY (map_role_id)
        REFERENCES role_master (pk_role_id) ON DELETE CASCADE,
    CONSTRAINT fk_map_module_id FOREIGN KEY (map_module_id)
        REFERENCES module_master (pk_module_id) ON DELETE CASCADE,
    CONSTRAINT fk_map_permission_id FOREIGN KEY (map_permission_id)
        REFERENCES permission_master (pk_permission_id) ON DELETE CASCADE
);
CREATE INDEX fk_map_role_id ON role_permission_mapping (map_role_id);
CREATE INDEX fk_map_module_id ON role_permission_mapping (map_module_id);
CREATE INDEX fk_map_permission_id ON role_permission_mapping (map_permission_id);
