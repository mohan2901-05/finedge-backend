package com.finedge.etfdb;

import com.finedge.entity.ModuleMaster;
import com.finedge.entity.Organization;
import com.finedge.entity.PermissionMaster;
import com.finedge.entity.RoleMaster;
import com.finedge.entity.RolePermissionMapping;
import com.finedge.entity.UserInfo;
import com.finedge.repository.ModuleMasterRepository;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.PermissionMasterRepository;
import com.finedge.repository.RoleMasterRepository;
import com.finedge.repository.RolePermissionMappingRepository;
import com.finedge.repository.UserInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2 verification for the six ETFDB IAM tables (now the canonical
 * {@code db/migration} chain V1/V2, so no location override is needed).
 *
 * <p>IDs are caller-supplied {@code U}-suffixed literals to avoid colliding
 * with V2 fabricated seed ({@code *-T*}). MySQL-specific behavior
 * ({@code ON UPDATE CURRENT_TIMESTAMP}) is NOT asserted here; it requires
 * the {@code etfdb} profile against {@code etfdb_local} MySQL (see M2
 * report for the exact command).
 */
@SpringBootTest
@Transactional
class EtfdbIamRepositoryTest {

    @Autowired
    private OrganizationRepository organizations;

    @Autowired
    private RoleMasterRepository roles;

    @Autowired
    private UserInfoRepository users;

    @Autowired
    private ModuleMasterRepository modules;

    @Autowired
    private PermissionMasterRepository permissions;

    @Autowired
    private RolePermissionMappingRepository mappings;

    @Test
    @DisplayName("M1: org -> role -> user -> module/permission -> mapping CRUD")
    void iam_CrudRoundTrip() {
        Organization org = new Organization();
        org.setOrgId("ORG-U001");
        org.setOrgName("Unit Org");
        org.setOrgEmail("unit-org@example.invalid");
        organizations.save(org);

        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-U01");
        role.setOrganization(org);
        role.setRoleName("Unit-Role");
        roles.save(role);

        UserInfo user = new UserInfo();
        user.setUserId("USR-U001");
        user.setOrganization(org);
        user.setUserName("Unit User");
        user.setPassword("{bcrypt}UNIT-PLACEHOLDER");
        user.setUserEmail("unit-user@example.invalid");
        user.setRole(role);
        users.save(user);

        ModuleMaster module = new ModuleMaster();
        module.setModuleId("MOD-U01");
        module.setModuleName("Unit-Module");
        module.setModuleCode("UNIT_MODULE");
        modules.save(module);

        PermissionMaster permission = new PermissionMaster();
        permission.setPermissionId("PERM-U01");
        permission.setPermissionName("Unit-Perm");
        permission.setPermissionCode("UNIT_PERM");
        permissions.save(permission);

        RolePermissionMapping mapping = new RolePermissionMapping();
        mapping.setMappingId("MAP-U001");
        mapping.setOrganization(org);
        mapping.setRole(role);
        mapping.setModule(module);
        mapping.setPermission(permission);
        mappings.save(mapping);

        assertTrue(organizations.findById("ORG-U001").isPresent());
        assertTrue(users.findByUserEmail("unit-user@example.invalid").isPresent());
        assertEquals(1, mappings.findByRole_RoleId("ROLE-U01").size());
        assertTrue(mappings.existsByOrganization_OrgIdAndRole_RoleIdAndModule_ModuleIdAndPermission_PermissionId(
                "ORG-U001", "ROLE-U01", "MOD-U01", "PERM-U01"));
    }

    @Test
    @DisplayName("M1: duplicate user email violates unique constraint")
    void iam_DuplicateEmailRejected() {
        Organization org = new Organization();
        org.setOrgId("ORG-U002");
        org.setOrgName("Unit Org 2");
        org.setOrgEmail("unit-org-2@example.invalid");
        organizations.save(org);

        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-U02");
        role.setOrganization(org);
        role.setRoleName("Unit-Role-2");
        roles.save(role);

        UserInfo first = new UserInfo();
        first.setUserId("USR-U002");
        first.setOrganization(org);
        first.setUserName("First");
        first.setPassword("{bcrypt}UNIT-PLACEHOLDER");
        first.setUserEmail("dup-user@example.invalid");
        first.setRole(role);
        users.saveAndFlush(first);

        UserInfo second = new UserInfo();
        second.setUserId("USR-U003");
        second.setOrganization(org);
        second.setUserName("Second");
        second.setPassword("{bcrypt}UNIT-PLACEHOLDER");
        second.setUserEmail("dup-user@example.invalid");
        second.setRole(role);
        assertThrows(DataIntegrityViolationException.class, () -> users.saveAndFlush(second));
    }

    @Test
    @DisplayName("M1: duplicate role-permission mapping violates uk_tenant_security")
    void iam_DuplicateMappingRejected() {
        Organization org = new Organization();
        org.setOrgId("ORG-U003");
        org.setOrgName("Unit Org 3");
        org.setOrgEmail("unit-org-3@example.invalid");
        organizations.save(org);

        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-U03");
        role.setOrganization(org);
        role.setRoleName("Unit-Role-3");
        roles.save(role);

        ModuleMaster module = new ModuleMaster();
        module.setModuleId("MOD-U03");
        module.setModuleName("Unit-Module-3");
        module.setModuleCode("UNIT_MODULE_3");
        modules.save(module);

        PermissionMaster permission = new PermissionMaster();
        permission.setPermissionId("PERM-U03");
        permission.setPermissionName("Unit-Perm-3");
        permission.setPermissionCode("UNIT_PERM_3");
        permissions.save(permission);

        RolePermissionMapping first = new RolePermissionMapping();
        first.setMappingId("MAP-U003");
        first.setOrganization(org);
        first.setRole(role);
        first.setModule(module);
        first.setPermission(permission);
        mappings.saveAndFlush(first);

        RolePermissionMapping second = new RolePermissionMapping();
        second.setMappingId("MAP-U004");
        second.setOrganization(org);
        second.setRole(role);
        second.setModule(module);
        second.setPermission(permission);
        assertThrows(DataIntegrityViolationException.class, () -> mappings.saveAndFlush(second));
    }
}
