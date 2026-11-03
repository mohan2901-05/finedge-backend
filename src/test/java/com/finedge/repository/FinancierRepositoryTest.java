package com.finedge.repository;

import com.finedge.entity.Financier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2 Part C repository integration test for {@code financiers}.
 * Runs against H2 with Flyway migrations V1-V5 applied.
 * Each test uses globally unique institution names to avoid unique-key
 * conflicts since the H2 database is shared across tests in the class.
 */
@SpringBootTest
@Transactional
class FinancierRepositoryTest {

    @Autowired
    private FinancierRepository financierRepository;

    private Financier save(String id, String name, String status) {
        Financier f = new Financier();
        f.setFinancierId(id);
        f.setInstitutionName(name);
        f.setDescription("Test financier " + name);
        f.setIndicativeAnnualRate(new BigDecimal("12.50"));
        f.setMinLoanAmount(new BigDecimal("50000.00"));
        f.setMaxLoanAmount(new BigDecimal("5000000.00"));
        f.setAvailableTenureMonths(24);
        f.setStatus(status);
        return financierRepository.save(f);
    }

    @Test
    @DisplayName("Save and find financier by ID")
    void saveAndFindById() {
        Financier saved = save("FIN-R-001", "Repo Alpha Bank", "ACTIVE");
        assertNotNull(saved.getFinancierId());

        Financier found = financierRepository.findById("FIN-R-001").orElseThrow();
        assertEquals("Repo Alpha Bank", found.getInstitutionName());
        assertEquals("ACTIVE", found.getStatus());
        assertEquals(new BigDecimal("12.50"), found.getIndicativeAnnualRate());
    }

    @Test
    @DisplayName("Find by institution name")
    void findByInstitutionName() {
        save("FIN-R-002", "Repo Beta Finance", "ACTIVE");

        Financier found = financierRepository.findByInstitutionName("Repo Beta Finance").orElseThrow();
        assertEquals("FIN-R-002", found.getFinancierId());
    }

    @Test
    @DisplayName("Find by status returns only matching financiers")
    void findByStatus() {
        save("FIN-R-003", "Repo Gamma Lending", "ACTIVE");
        save("FIN-R-004", "Repo Delta Credit", "INACTIVE");

        Page<Financier> active = financierRepository.findByStatus("ACTIVE", PageRequest.of(0, 10));
        assertEquals(1, active.getTotalElements());
        assertEquals("Repo Gamma Lending", active.getContent().get(0).getInstitutionName());

        Page<Financier> inactive = financierRepository.findByStatus("INACTIVE", PageRequest.of(0, 10));
        assertEquals(1, inactive.getTotalElements());
        assertEquals("Repo Delta Credit", inactive.getContent().get(0).getInstitutionName());
    }

    @Test
    @DisplayName("Search by institution name containing (case-insensitive)")
    void searchByInstitutionNameContaining() {
        save("FIN-R-005", "Repo Omega Alpha", "ACTIVE");
        save("FIN-R-006", "Repo alpha-beta finance", "ACTIVE");
        save("FIN-R-007", "Repo Zeta Lending", "ACTIVE");

        Page<Financier> result = financierRepository
                .findByInstitutionNameContainingIgnoreCase("alpha", PageRequest.of(0, 10));
        assertEquals(2, result.getTotalElements());
    }

    @Test
    @DisplayName("Duplicate institution name violates unique constraint")
    void duplicateInstitutionNameRejected() {
        save("FIN-R-008", "Repo Duplicate Bank", "ACTIVE");

        Financier dup = new Financier();
        dup.setFinancierId("FIN-R-009");
        dup.setInstitutionName("Repo Duplicate Bank");
        dup.setStatus("ACTIVE");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> financierRepository.saveAndFlush(dup))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exists by institution name")
    void existsByInstitutionName() {
        save("FIN-R-010", "Repo Exists Bank", "ACTIVE");
        assertTrue(financierRepository.existsByInstitutionName("Repo Exists Bank"));
        assertFalse(financierRepository.existsByInstitutionName("Repo Nonexistent Bank"));
    }
}
