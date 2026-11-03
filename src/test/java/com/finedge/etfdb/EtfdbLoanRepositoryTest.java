package com.finedge.etfdb;

import com.finedge.entity.AuditLog;
import com.finedge.entity.LoanActivityStatus;
import com.finedge.entity.LoanApplication;
import com.finedge.entity.LoanCollection;
import com.finedge.entity.LoanInfo;
import com.finedge.entity.Organization;
import com.finedge.entity.RepaymentDetail;
import com.finedge.entity.RoleMaster;
import com.finedge.entity.UserInfo;
import com.finedge.repository.AuditLogRepository;
import com.finedge.repository.LoanActivityStatusRepository;
import com.finedge.repository.LoanApplicationRepository;
import com.finedge.repository.LoanCollectionRepository;
import com.finedge.repository.LoanInfoRepository;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.RepaymentDetailRepository;
import com.finedge.repository.RoleMasterRepository;
import com.finedge.repository.UserInfoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2 verification for the six loan/audit tables: intake (auto-increment),
 * loan + activity + schedule + collection + audit, including the
 * {@code ON DELETE SET NULL} collector rule and the unique intake
 * {@code ref}. IDs use {@code V}-suffixed literals to avoid the V2/V4
 * fabricated seeds.
 */
@SpringBootTest
@Transactional
class EtfdbLoanRepositoryTest {

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
    private RepaymentDetailRepository repayments;

    @Autowired
    private LoanCollectionRepository collections;

    @Autowired
    private AuditLogRepository audits;

    @PersistenceContext
    private EntityManager entityManager;

    private UserInfo borrowerFixture(String suffix) {
        Organization org = new Organization();
        org.setOrgId("ORG-V" + suffix);
        org.setOrgName("Loan Org " + suffix);
        org.setOrgEmail("loan-org-" + suffix + "@example.invalid");
        organizations.save(org);

        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-V" + suffix);
        role.setOrganization(org);
        role.setRoleName("Borrower " + suffix);
        roles.save(role);

        UserInfo borrower = new UserInfo();
        borrower.setUserId("USR-V" + suffix);
        borrower.setOrganization(org);
        borrower.setUserName("Borrower " + suffix);
        borrower.setPassword("{bcrypt}UNIT-PLACEHOLDER");
        borrower.setUserEmail("borrower-" + suffix + "@example.invalid");
        borrower.setRole(role);
        users.save(borrower);
        return borrower;
    }

    @Test
    @DisplayName("M2: intake auto-increment + loan lifecycle + ordered schedule")
    void loan_LifecycleRoundTrip() {
        UserInfo borrower = borrowerFixture("001");

        LoanApplication intake = new LoanApplication();
        intake.setRef("TEST-REF-V001");
        intake.setCustomerName("Borrower 001");
        intake.setCustomerPhone("9000000099");
        intake.setCustomerEmail("borrower-001@example.invalid");
        intake.setCustomerCity("Test City");
        intake.setCustomerAddress("9 Test Road");
        intake.setFinancierKey("test_fin");
        intake.setFinancierLabel("Test Finance");
        intake.setLoanAmount(new BigDecimal("60000.00"));
        intake.setTenureMonths(12);
        intake.setInterestRate(new BigDecimal("12.00"));
        intake.setEmi(new BigDecimal("5330.93"));
        intake.setTotalInterest(new BigDecimal("3971.16"));
        intake.setTotalPayable(new BigDecimal("63971.16"));
        applications.save(intake);
        assertTrue(intake.getId() != null && intake.getId() > 0,
                "Intake ID must be auto-generated");
        assertTrue(applications.findByRef("TEST-REF-V001").isPresent());

        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-V001");
        loan.setOrganization(borrower.getOrganization());
        loan.setUser(borrower);
        loan.setLoanAmount(new BigDecimal("60000.00"));
        loan.setInterestRate(new BigDecimal("12.00"));
        loan.setTenureMonths(12);
        loan.setStatus("DISBURSED");
        loans.save(loan);
        assertTrue(loan.isReceived() == false);
        assertEquals(1, loans.findByUser_UserId(borrower.getUserId()).size());

        LoanActivityStatus activity = new LoanActivityStatus();
        activity.setActivityId("LAS-V001");
        activity.setUser(borrower);
        activity.setLoan(loan);
        activity.setActivityCode("DID");
        activity.setStatusCode("D");
        activities.save(activity);
        assertEquals(1, activities.findByLoan_LoanId("LOAN-V001").size());

        RepaymentDetail first = new RepaymentDetail();
        first.setRepaymentId("REP-V001");
        first.setLoan(loan);
        first.setInstallmentNumber(2);
        first.setDueDate(LocalDate.of(2026, 3, 7));
        first.setOpeningPrincipal(new BigDecimal("55283.67"));
        first.setInstallmentAmount(new BigDecimal("5330.93"));
        first.setPrincipal(new BigDecimal("4778.09"));
        first.setInterest(new BigDecimal("552.84"));
        first.setClosingPrincipal(new BigDecimal("50505.58"));
        first.setRate(new BigDecimal("12.00"));
        repayments.save(first);

        RepaymentDetail second = new RepaymentDetail();
        second.setRepaymentId("REP-V002");
        second.setLoan(loan);
        second.setInstallmentNumber(1);
        second.setDueDate(LocalDate.of(2026, 2, 7));
        second.setOpeningPrincipal(new BigDecimal("60000.00"));
        second.setInstallmentAmount(new BigDecimal("5330.93"));
        second.setPrincipal(new BigDecimal("4730.93"));
        second.setInterest(new BigDecimal("600.00"));
        second.setClosingPrincipal(new BigDecimal("55283.67"));
        second.setRate(new BigDecimal("12.00"));
        repayments.save(second);

        assertEquals("REP-V002",
                repayments.findByLoan_LoanIdOrderByInstallmentNumberAsc("LOAN-V001")
                        .get(0).getRepaymentId(),
                "Schedule must come back in installment order");

        AuditLog audit = new AuditLog();
        audit.setAuditId("AUD-V001");
        audit.setOrganization(borrower.getOrganization());
        audit.setUser(borrower);
        audit.setUserName(borrower.getUserName());
        audit.setAction("DISBURSE_LOAN");
        audit.setDetails("Unit disbursal of LOAN-V001");
        audits.save(audit);
        assertEquals(1, audits.findByUser_UserIdOrderByCreatedDatetimeDesc(
                borrower.getUserId()).size());
    }

    @Test
    @DisplayName("M2: duplicate intake ref violates unique constraint")
    void loan_DuplicateRefRejected() {
        LoanApplication first = minimalIntake("TEST-REF-V002");
        applications.saveAndFlush(first);

        LoanApplication second = minimalIntake("TEST-REF-V002");
        assertThrows(DataIntegrityViolationException.class,
                () -> applications.saveAndFlush(second));
    }

    @Test
    @DisplayName("M2: deleting the collecting agent SETs NULL instead of the row")
    void loan_CollectorDeleteSetsNull() {
        UserInfo borrower = borrowerFixture("002");

        UserInfo agent = new UserInfo();
        agent.setUserId("USR-VAG2");
        agent.setOrganization(borrower.getOrganization());
        agent.setUserName("Agent 002");
        agent.setPassword("{bcrypt}UNIT-PLACEHOLDER");
        agent.setUserEmail("agent-002@example.invalid");
        agent.setRole(borrower.getRole());
        users.save(agent);

        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-V002");
        loan.setOrganization(borrower.getOrganization());
        loan.setUser(borrower);
        loan.setLoanAmount(new BigDecimal("10000.00"));
        loan.setInterestRate(new BigDecimal("12.00"));
        loan.setTenureMonths(6);
        loans.save(loan);

        LoanCollection collection = new LoanCollection();
        collection.setCollectionId("COLL-V002");
        collection.setOrganization(borrower.getOrganization());
        collection.setLoan(loan);
        collection.setUser(borrower);
        collection.setCollectionAmount(new BigDecimal("1000.00"));
        collection.setCollectedBy(agent);
        collections.saveAndFlush(collection);

        users.delete(agent);
        users.flush();

        // The managed LoanCollection still holds the old reference; clear the
        // persistence context so the assertion reads the database state where
        // ON DELETE SET NULL has fired.
        entityManager.clear();

        LoanCollection reloaded =
                collections.findById("COLL-V002").orElseThrow();
        assertNull(reloaded.getCollectedBy(),
                "Collector FK must SET NULL when the agent is deleted");
    }

    private static LoanApplication minimalIntake(String ref) {
        LoanApplication intake = new LoanApplication();
        intake.setRef(ref);
        intake.setCustomerName("Test");
        intake.setCustomerPhone("9000000098");
        intake.setCustomerEmail("intake@example.invalid");
        intake.setCustomerCity("Test City");
        intake.setCustomerAddress("8 Test Road");
        intake.setFinancierKey("test_fin");
        intake.setFinancierLabel("Test Finance");
        intake.setLoanAmount(new BigDecimal("10000.00"));
        intake.setTenureMonths(6);
        intake.setInterestRate(new BigDecimal("12.00"));
        intake.setEmi(new BigDecimal("1000.00"));
        intake.setTotalInterest(new BigDecimal("500.00"));
        intake.setTotalPayable(new BigDecimal("10500.00"));
        return intake;
    }
}
