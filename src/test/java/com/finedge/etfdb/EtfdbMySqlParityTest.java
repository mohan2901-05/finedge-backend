package com.finedge.etfdb;

import com.finedge.entity.AuditLog;
import com.finedge.entity.LoanActivityStatus;
import com.finedge.entity.LoanApplication;
import com.finedge.entity.LoanCollection;
import com.finedge.entity.LoanInfo;
import com.finedge.entity.Organization;
import com.finedge.entity.RoleMaster;
import com.finedge.entity.UserInfo;
import com.finedge.repository.AuditLogRepository;
import com.finedge.repository.LoanActivityStatusRepository;
import com.finedge.repository.LoanApplicationRepository;
import com.finedge.repository.LoanCollectionRepository;
import com.finedge.repository.LoanInfoRepository;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.RoleMasterRepository;
import com.finedge.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * MySQL 8 parity gate for the ETFDB schema (runs ONLY against local
 * {@code etfdb_local}; skipped otherwise).
 *
 * <p>Isolation contract:
 * <ul>
 *   <li>Gated by {@code ETFDB_MYSQL_TESTS=true} AND the {@code etfdb} profile,
 *       so default {@code mvn test} never needs MySQL.</li>
 *   <li>{@link #refuseUnlessLocalEtfdb()} fails closed unless the resolved
 *       JDBC URL points at {@code etfdb_local} on localhost/loopback — no
 *       shared, remote, or production database can be touched. The guard
 *       message never includes credentials.</li>
 *   <li>Every test builds its own {@code Q*} fixtures (V2/V4 seeds use
 *       {@code T*} IDs and are never read for assertions nor written).</li>
 *   <li>{@code @Transactional} rolls everything back, including the native
 *       {@link JdbcTemplate} DML (same DataSource/transaction). No DDL is
 *       issued. Flyway only validates (schema is already v4).</li>
 * </ul>
 *
 * <p>Raw {@link JdbcTemplate} SQL — never the persistence context — is used
 * wherever <em>MySQL itself</em> (CASCADE, SET NULL, defaults, ON UPDATE,
 * driver value mapping) is under test, so Hibernate behavior cannot mask
 * database behavior.
 */
@ActiveProfiles("etfdb")
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "ETFDB_MYSQL_TESTS", matches = "true")
class EtfdbMySqlParityTest {

    private static final Pattern MYSQL_HOST =
            Pattern.compile("^jdbc:mysql://([^/:\\[]+|\\[[^\\]]+\\])");

    @Autowired
    private Environment environment;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OrganizationRepository organizations;

    @Autowired
    private RoleMasterRepository roles;

    @Autowired
    private UserInfoRepository users;

    @Autowired
    private LoanApplicationRepository applications;

    @Autowired
    private LoanInfoRepository loans;

    @Autowired
    private LoanActivityStatusRepository activities;

    @Autowired
    private LoanCollectionRepository collections;

    @Autowired
    private AuditLogRepository audits;

    @BeforeEach
    void refuseUnlessLocalEtfdb() {
        String url = environment.getProperty("spring.datasource.url");
        if (url == null || !url.startsWith("jdbc:mysql://")) {
            fail("Refusing MySQL parity tests: expected a MySQL JDBC URL but got '"
                    + url + "'");
        }
        Matcher host = MYSQL_HOST.matcher(url);
        String hostname = host.find() ? host.group(1).replace("[", "").replace("]", "") : "";
        boolean loopback = hostname.equalsIgnoreCase("localhost")
                || hostname.equals("127.0.0.1")
                || hostname.equals("::1");
        String database = url.split("\\?", 2)[0];
        boolean etfdbLocal = database.endsWith("/etfdb_local");
        if (!loopback || !etfdbLocal) {
            fail("Refusing MySQL parity tests: JDBC URL must target etfdb_local "
                    + "on localhost, but was '" + database + "'");
        }
    }

    // -- fixture builders (parents before dependents, Q* IDs only) ----------
    // NOTE: saveAndFlush (not save) is deliberate. These fixtures are verified
    // with native JdbcTemplate SQL, and Hibernate's FlushMode.AUTO flushes
    // only before JPQL/Criteria queries -- never before raw JDBC. Without an
    // explicit flush the rows would be invisible to the native statements.

    private Organization newOrg(String suffix) {
        Organization org = new Organization();
        org.setOrgId("ORG-Q" + suffix);
        org.setOrgName("Parity Org " + suffix);
        org.setOrgEmail("parity-org-" + suffix + "@example.invalid");
        return organizations.saveAndFlush(org);
    }

    private RoleMaster newRole(Organization org, String suffix) {
        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-Q" + suffix);
        role.setOrganization(org);
        role.setRoleName("Parity Role " + suffix);
        return roles.saveAndFlush(role);
    }

    private UserInfo newUser(Organization org, RoleMaster role, String suffix, String email) {
        UserInfo user = new UserInfo();
        user.setUserId("USR-Q" + suffix);
        user.setOrganization(org);
        user.setUserName("Parity User " + suffix);
        user.setPassword("{bcrypt}PARITY-PLACEHOLDER");
        user.setUserEmail(email);
        user.setRole(role);
        return users.saveAndFlush(user);
    }

    private LoanInfo newLoan(Organization org, UserInfo borrower, String suffix) {
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-Q" + suffix);
        loan.setOrganization(org);
        loan.setUser(borrower);
        loan.setLoanAmount(new BigDecimal("50000.00"));
        loan.setInterestRate(new BigDecimal("12.00"));
        loan.setTenureMonths(12);
        loan.setStatus("DISBURSED");
        return loans.saveAndFlush(loan);
    }

    // -- tests ---------------------------------------------------------------

    @Test
    @DisplayName("MySQL: received reads/writes as numeric 0/1 (not Boolean)")
    void received_NumericZeroOne() {
        Organization org = newOrg("101");
        RoleMaster role = newRole(org, "101");
        UserInfo borrower = newUser(org, role, "101", "parity-101@example.invalid");
        LoanInfo loan = newLoan(org, borrower, "101");

        Integer storedZero = jdbc.queryForObject(
                "SELECT received FROM loan_info WHERE pk_loan_id=?", Integer.class, "LOAN-Q101");
        assertEquals(0, storedZero);
        assertTrue(!loans.findById("LOAN-Q101").orElseThrow().isReceived());

        loan.setReceived(true);
        loans.saveAndFlush(loan);

        Integer storedOne = jdbc.queryForObject(
                "SELECT received FROM loan_info WHERE pk_loan_id=?", Integer.class, "LOAN-Q101");
        assertEquals(1, storedOne);
        assertTrue(loans.findById("LOAN-Q101").orElseThrow().isReceived());
    }

    @Test
    @DisplayName("MySQL: deleting an org cascades to children via FK rules")
    void cascade_OrgDeleteRemovesChildren() {
        Organization org = newOrg("102");
        RoleMaster role = newRole(org, "102");
        UserInfo borrower = newUser(org, role, "102", "parity-102@example.invalid");
        LoanInfo loan = newLoan(org, borrower, "102");

        LoanActivityStatus activity = new LoanActivityStatus();
        activity.setActivityId("LAS-Q102");
        activity.setUser(borrower);
        activity.setLoan(loan);
        activity.setActivityCode("BDE");
        activity.setStatusCode("A");
        activities.save(activity);

        AuditLog audit = new AuditLog();
        audit.setAuditId("AUD-Q102");
        audit.setOrganization(org);
        audit.setUser(borrower);
        audit.setUserName(borrower.getUserName());
        audit.setAction("PARITY_CHECK");
        audits.save(audit);

        // Pure-SQL delete: only the database's ON DELETE CASCADE may act.
        int deleted = jdbc.update("DELETE FROM org_info WHERE pk_org_id=?", "ORG-Q102");
        assertEquals(1, deleted);

        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_master WHERE pk_role_id=?", Integer.class, "ROLE-Q102"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_info WHERE pk_user_id=?", Integer.class, "USR-Q102"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM loan_info WHERE pk_loan_id=?", Integer.class, "LOAN-Q102"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM loan_activity_status WHERE pk_las_id=?", Integer.class, "LAS-Q102"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE pk_audit_id=?", Integer.class, "AUD-Q102"));
    }

    @Test
    @DisplayName("MySQL: deleting the collecting agent SETs NULL, keeps the row")
    void setNull_CollectorDeleteKeepsRow() {
        Organization org = newOrg("103");
        RoleMaster role = newRole(org, "103");
        UserInfo borrower = newUser(org, role, "103", "parity-103@example.invalid");
        UserInfo agent = newUser(org, role, "9A3", "parity-agent@example.invalid");
        LoanInfo loan = newLoan(org, borrower, "103");

        LoanCollection collection = new LoanCollection();
        collection.setCollectionId("COLL-Q103");
        collection.setOrganization(org);
        collection.setLoan(loan);
        collection.setUser(borrower);
        collection.setCollectionAmount(new BigDecimal("1000.00"));
        collection.setCollectedBy(agent);
        collections.saveAndFlush(collection);

        int deleted = jdbc.update("DELETE FROM user_info WHERE pk_user_id=?", "USR-Q9A3");
        assertEquals(1, deleted);

        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM loan_collection WHERE pk_collection_id=?", Integer.class, "COLL-Q103");
        assertEquals(1, rows);
        assertNull(jdbc.queryForObject(
                "SELECT collected_by_user_id FROM loan_collection WHERE pk_collection_id=?",
                String.class, "COLL-Q103"));
    }

    @Test
    @DisplayName("MySQL: ON UPDATE CURRENT_TIMESTAMP fires on native UPDATE")
    void timestamp_OnUpdateFires() throws InterruptedException {
        jdbc.update("INSERT INTO org_info (pk_org_id, org_name, org_email) VALUES (?,?,?)",
                "ORG-Q104", "Parity Org 104", "parity-104@example.invalid");

        LocalDateTime before = jdbc.queryForObject(
                "SELECT updated_datetime FROM org_info WHERE pk_org_id=?",
                LocalDateTime.class, "ORG-Q104");

        // MySQL DATETIME defaults to 1-second resolution: wait past a tick.
        Thread.sleep(1200);
        int updated = jdbc.update(
                "UPDATE org_info SET org_name=? WHERE pk_org_id=?", "Parity Org 104b", "ORG-Q104");
        assertEquals(1, updated);

        LocalDateTime after = jdbc.queryForObject(
                "SELECT updated_datetime FROM org_info WHERE pk_org_id=?",
                LocalDateTime.class, "ORG-Q104");
        assertTrue(after.isAfter(before),
                "updated_datetime must advance via ON UPDATE CURRENT_TIMESTAMP");
    }

    @Test
    @DisplayName("MySQL: DEFAULT (curdate()) fills omitted DATE columns")
    void defaults_CurrentDateFills() {
        Organization org = newOrg("105");
        RoleMaster role = newRole(org, "105");
        newUser(org, role, "105", "parity-105@example.invalid"); // side-effect: inserts row

        // Native inserts omit the date columns entirely so @PrePersist cannot
        // mask the database defaults under test.
        jdbc.update("INSERT INTO loan_info (pk_loan_id, loan_org_id, loan_user_id,"
                        + " loan_amount, tenure_months) VALUES (?,?,?,?,?)",
                "LOAN-Q105", "ORG-Q105", "USR-Q105", new BigDecimal("20000.00"), 6);
        jdbc.update("INSERT INTO loan_collection (pk_collection_id, collection_org_id,"
                        + " collection_loan_id, collection_user_id, collection_amount)"
                        + " VALUES (?,?,?,?,?)",
                "COLL-Q105", "ORG-Q105", "LOAN-Q105", "USR-Q105", new BigDecimal("500.00"));

        LocalDate today = LocalDate.now();
        assertEquals(today, jdbc.queryForObject(
                "SELECT application_date FROM loan_info WHERE pk_loan_id=?",
                LocalDate.class, "LOAN-Q105"));
        assertEquals(today, jdbc.queryForObject(
                "SELECT collection_date FROM loan_collection WHERE pk_collection_id=?",
                LocalDate.class, "COLL-Q105"));
    }

    @Test
    @DisplayName("MySQL: intake auto-increment and unique ref")
    void crud_IntakeAutoIncrementAndConstraints() {
        LoanApplication intake = new LoanApplication();
        intake.setRef("TEST-REF-Q106");
        intake.setCustomerName("Parity Borrower");
        intake.setCustomerPhone("9000000106");
        intake.setCustomerEmail("parity-intake@example.invalid");
        intake.setCustomerCity("Parity City");
        intake.setCustomerAddress("106 Parity Road");
        intake.setFinancierKey("parity_fin");
        intake.setFinancierLabel("Parity Finance");
        intake.setLoanAmount(new BigDecimal("30000.00"));
        intake.setTenureMonths(6);
        intake.setInterestRate(new BigDecimal("12.00"));
        intake.setEmi(new BigDecimal("1000.00"));
        intake.setTotalInterest(new BigDecimal("500.00"));
        intake.setTotalPayable(new BigDecimal("30500.00"));
        applications.save(intake);
        assertTrue(intake.getId() != null && intake.getId() > 0,
                "Intake ID must be auto-generated");
        assertTrue(applications.findByRef("TEST-REF-Q106").isPresent());

        LoanApplication duplicate = new LoanApplication();
        duplicate.setRef("TEST-REF-Q106");
        duplicate.setCustomerName("Parity Borrower");
        duplicate.setCustomerPhone("9000000107");
        duplicate.setCustomerEmail("parity-intake2@example.invalid");
        duplicate.setCustomerCity("Parity City");
        duplicate.setCustomerAddress("107 Parity Road");
        duplicate.setFinancierKey("parity_fin");
        duplicate.setFinancierLabel("Parity Finance");
        duplicate.setLoanAmount(new BigDecimal("30000.00"));
        duplicate.setTenureMonths(6);
        duplicate.setInterestRate(new BigDecimal("12.00"));
        duplicate.setEmi(new BigDecimal("1000.00"));
        duplicate.setTotalInterest(new BigDecimal("500.00"));
        duplicate.setTotalPayable(new BigDecimal("30500.00"));
        assertThrows(DataIntegrityViolationException.class,
                () -> applications.saveAndFlush(duplicate));
    }

    @Test
    @DisplayName("MySQL: loan with a nonexistent org fails on the FK")
    void crud_BadForeignKeyRejected() {
        // Separate test method on purpose: the duplicate-ref violation above
        // leaves that test's Hibernate session unusable ("never reuse a
        // Session after an exception"), so the FK violation needs a clean
        // session to surface as DataIntegrityViolationException.
        Organization org = newOrg("107");
        RoleMaster role = newRole(org, "107");
        UserInfo borrower = newUser(org, role, "107", "parity-107@example.invalid");

        // getReferenceById yields a proxy without a SELECT, so the violation
        // surfaces from the database at flush (a transient instance would
        // fail earlier inside Hibernate instead).
        Organization ghost = organizations.getReferenceById("ORG-QXXX");
        LoanInfo orphan = new LoanInfo();
        orphan.setLoanId("LOAN-QXXX");
        orphan.setOrganization(ghost);
        orphan.setUser(borrower);
        orphan.setLoanAmount(new BigDecimal("1000.00"));
        orphan.setTenureMonths(6);
        assertThrows(DataIntegrityViolationException.class,
                () -> loans.saveAndFlush(orphan));
    }
}
