package com.finedge.service;

import com.finedge.dto.FinancierProfile;
import com.finedge.dto.FinancierProfileCreateRequest;
import com.finedge.dto.FinancierProfileUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.Financier;
import com.finedge.exception.BusinessRuleException;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.FinancierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancierServiceTest {

    @Mock
    private FinancierRepository financierRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @InjectMocks
    private FinancierService financierService;

    private FinancierProfileCreateRequest createRequest() {
        FinancierProfileCreateRequest request = new FinancierProfileCreateRequest();
        request.setInstitutionName("Alpha Capital");
        request.setDescription("Term lending specialist");
        request.setIndicativeAnnualRate(new BigDecimal("12.50"));
        request.setMinLoanAmount(new BigDecimal("50000.00"));
        request.setMaxLoanAmount(new BigDecimal("5000000.00"));
        request.setAvailableTenureMonths(24);
        request.setStatus("ACTIVE");
        return request;
    }

    private Financier entity(String id) {
        Financier f = new Financier();
        f.setFinancierId(id);
        f.setInstitutionName("Alpha Capital");
        f.setStatus("ACTIVE");
        return f;
    }

    @Test
    @DisplayName("Create assigns a generated ID and returns the DTO")
    void create_AssignsId() {
        when(financierRepository.existsByInstitutionName(eq("Alpha Capital"))).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("FIN-ABC123");
        when(financierRepository.save(any(Financier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FinancierProfile created = financierService.create(createRequest());

        assertNotNull(created.getFinancierId());
        assertEquals("FIN-ABC123", created.getFinancierId());
        assertEquals("Alpha Capital", created.getInstitutionName());
        assertEquals("ACTIVE", created.getStatus());
        verify(financierRepository).save(any(Financier.class));
    }

    @Test
    @DisplayName("Create with a duplicate institution name fails")
    void create_DuplicateInstitutionFails() {
        when(financierRepository.existsByInstitutionName(eq("Alpha Capital"))).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> financierService.create(createRequest()));
    }

    @Test
    @DisplayName("Create with min loan amount exceeding max loan amount fails")
    void create_MinExceedsMaxFails() {
        FinancierProfileCreateRequest request = createRequest();
        request.setMinLoanAmount(new BigDecimal("6000000.00"));
        request.setMaxLoanAmount(new BigDecimal("5000000.00"));
        when(financierRepository.existsByInstitutionName(eq("Alpha Capital"))).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("FIN-ABC123");

        assertThrows(BusinessRuleException.class,
                () -> financierService.create(request));
    }

    @Test
    @DisplayName("Create with invalid status fails")
    void create_InvalidStatusFails() {
        FinancierProfileCreateRequest request = createRequest();
        request.setStatus("FOOBAR");
        when(financierRepository.existsByInstitutionName(eq("Alpha Capital"))).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("FIN-ABC123");

        assertThrows(BusinessRuleException.class,
                () -> financierService.create(request));
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404 semantics")
    void getById_UnknownFails() {
        when(financierRepository.findById(eq("NOPE"))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> financierService.getById("NOPE"));
    }

    @Test
    @DisplayName("Get by ID returns the DTO")
    void getById_ReturnsDto() {
        Financier f = entity("FIN-1");
        f.setIndicativeAnnualRate(new BigDecimal("12.50"));
        f.setMinLoanAmount(new BigDecimal("50000.00"));
        f.setMaxLoanAmount(new BigDecimal("5000000.00"));
        f.setAvailableTenureMonths(24);
        when(financierRepository.findById(eq("FIN-1"))).thenReturn(Optional.of(f));

        FinancierProfile dto = financierService.getById("FIN-1");

        assertEquals("FIN-1", dto.getFinancierId());
        assertEquals("Alpha Capital", dto.getInstitutionName());
        assertEquals(new BigDecimal("12.50"), dto.getIndicativeAnnualRate());
        assertEquals(24, dto.getAvailableTenureMonths());
    }

    @Test
    @DisplayName("List returns only ACTIVE financiers by default")
    void list_DefaultActiveOnly() {
        Financier f = entity("FIN-1");
        f.setStatus("ACTIVE");
        when(financierRepository.findByStatus(eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f)));

        PageResponse<FinancierProfile> page = financierService.list(null, null, 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("FIN-1", page.getContent().get(0).getFinancierId());
        verify(financierRepository).findByStatus(eq("ACTIVE"), any(Pageable.class));
    }

    @Test
    @DisplayName("List with a status filter queries by status")
    void list_ByStatus() {
        Financier f = entity("FIN-1");
        f.setStatus("INACTIVE");
        when(financierRepository.findByStatus(eq("INACTIVE"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f)));

        PageResponse<FinancierProfile> page = financierService.list("INACTIVE", null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(financierRepository).findByStatus(eq("INACTIVE"), any(Pageable.class));
    }

    @Test
    @DisplayName("List with a search queries by institution name containing")
    void list_BySearch() {
        Financier f = entity("FIN-1");
        f.setStatus("ACTIVE");
        when(financierRepository.findByInstitutionNameContainingIgnoreCase(eq("Alpha"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f)));

        PageResponse<FinancierProfile> page = financierService.list(null, "Alpha", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(financierRepository).findByInstitutionNameContainingIgnoreCase(eq("Alpha"), any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(financierRepository.findByStatus(eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<FinancierProfile> page = financierService.list(null, null, 0, 10);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Update changes mutable fields")
    void update_Success() {
        Financier f = entity("FIN-1");
        f.setInstitutionName("Old Name");
        f.setStatus("ACTIVE");
        FinancierProfileUpdateRequest request = new FinancierProfileUpdateRequest();
        request.setInstitutionName(" New Name ");
        request.setStatus("INACTIVE");
        when(financierRepository.findById(eq("FIN-1"))).thenReturn(Optional.of(f));
        when(financierRepository.save(any(Financier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FinancierProfile updated = financierService.update("FIN-1", request);

        assertEquals("New Name", updated.getInstitutionName());
        assertEquals("INACTIVE", updated.getStatus());
    }

    @Test
    @DisplayName("Update to a taken institution name fails")
    void update_DuplicateInstitutionFails() {
        Financier f = entity("FIN-1");
        f.setInstitutionName("Old Name");
        FinancierProfileUpdateRequest request = new FinancierProfileUpdateRequest();
        request.setInstitutionName("Taken Name");
        when(financierRepository.findById(eq("FIN-1"))).thenReturn(Optional.of(f));
        when(financierRepository.existsByInstitutionName(eq("Taken Name"))).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> financierService.update("FIN-1", request));
    }

    @Test
    @DisplayName("Update with min loan amount exceeding max loan amount fails")
    void update_MinExceedsMaxFails() {
        Financier f = entity("FIN-1");
        f.setInstitutionName("Old Name");
        f.setMinLoanAmount(new BigDecimal("100000.00"));
        f.setMaxLoanAmount(new BigDecimal("500000.00"));
        FinancierProfileUpdateRequest request = new FinancierProfileUpdateRequest();
        request.setMinLoanAmount(new BigDecimal("600000.00"));
        when(financierRepository.findById(eq("FIN-1"))).thenReturn(Optional.of(f));

        assertThrows(BusinessRuleException.class,
                () -> financierService.update("FIN-1", request));
    }

    @Test
    @DisplayName("Update with invalid status fails")
    void update_InvalidStatusFails() {
        Financier f = entity("FIN-1");
        f.setInstitutionName("Old Name");
        FinancierProfileUpdateRequest request = new FinancierProfileUpdateRequest();
        request.setStatus("FOOBAR");
        when(financierRepository.findById(eq("FIN-1"))).thenReturn(Optional.of(f));

        assertThrows(BusinessRuleException.class,
                () -> financierService.update("FIN-1", request));
    }

    @Test
    @DisplayName("Update keeping the same institution name (case-insensitive) succeeds")
    void update_SameInstitutionAllows() {
        Financier f = entity("FIN-1");
        f.setInstitutionName("Old Name");
        FinancierProfileUpdateRequest request = new FinancierProfileUpdateRequest();
        request.setInstitutionName("OLD NAME");
        when(financierRepository.findById(eq("FIN-1"))).thenReturn(Optional.of(f));
        lenient().when(financierRepository.existsByInstitutionName(eq("OLD NAME"))).thenReturn(true);
        when(financierRepository.save(any(Financier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FinancierProfile updated = financierService.update("FIN-1", request);

        assertEquals("OLD NAME", updated.getInstitutionName());
        verify(financierRepository, org.mockito.Mockito.never()).existsByInstitutionName(anyString());
    }

    @Test
    @DisplayName("Update with null fields preserves existing values (partial update)")
    void update_NullFieldsPreserveExisting() {
        Financier f = entity("FIN-1");
        f.setInstitutionName("Old Name");
        f.setIndicativeAnnualRate(new BigDecimal("12.50"));
        f.setMinLoanAmount(new BigDecimal("50000.00"));
        f.setMaxLoanAmount(new BigDecimal("5000000.00"));
        f.setAvailableTenureMonths(24);
        f.setStatus("ACTIVE");
        FinancierProfileUpdateRequest request = new FinancierProfileUpdateRequest();
        request.setStatus("INACTIVE");
        when(financierRepository.findById(eq("FIN-1"))).thenReturn(Optional.of(f));
        when(financierRepository.save(any(Financier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FinancierProfile updated = financierService.update("FIN-1", request);

        assertEquals("INACTIVE", updated.getStatus());
        assertEquals("Old Name", updated.getInstitutionName());
        assertEquals(new BigDecimal("12.50"), updated.getIndicativeAnnualRate());
        assertEquals(new BigDecimal("50000.00"), updated.getMinLoanAmount());
        assertEquals(new BigDecimal("5000000.00"), updated.getMaxLoanAmount());
        assertEquals(24, updated.getAvailableTenureMonths());
    }

    @Test
    @DisplayName("Update with an unknown ID fails with 404")
    void update_UnknownFails() {
        FinancierProfileUpdateRequest request = new FinancierProfileUpdateRequest();
        request.setInstitutionName("New");
        when(financierRepository.findById(eq("NOPE"))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> financierService.update("NOPE", request));
    }

    @Test
    @DisplayName("Deactivate sets status to INACTIVE (soft delete)")
    void deactivate_SetsInactive() {
        Financier f = entity("FIN-1");
        f.setStatus("ACTIVE");
        when(financierRepository.findById(eq("FIN-1"))).thenReturn(Optional.of(f));
        when(financierRepository.save(any(Financier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        financierService.deactivate("FIN-1");

        assertEquals("INACTIVE", f.getStatus());
        verify(financierRepository).save(f);
    }

    @Test
    @DisplayName("Deactivate with an unknown ID fails with 404")
    void deactivate_UnknownFails() {
        when(financierRepository.findById(eq("NOPE"))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> financierService.deactivate("NOPE"));
    }
}
