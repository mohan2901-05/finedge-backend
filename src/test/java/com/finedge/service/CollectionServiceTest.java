package com.finedge.service;

import com.finedge.dto.LoanCollectionCreateRequest;
import com.finedge.dto.LoanCollectionDTO;
import com.finedge.dto.LoanCollectionUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.LoanCollection;
import com.finedge.entity.LoanInfo;
import com.finedge.entity.Organization;
import com.finedge.entity.UserInfo;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.LoanCollectionRepository;
import com.finedge.repository.LoanInfoRepository;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.UserInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private LoanCollectionRepository collectionRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private LoanInfoRepository loanRepository;

    @Mock
    private UserInfoRepository userRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @InjectMocks
    private CollectionService collectionService;

    private LoanCollectionCreateRequest createRequest() {
        LoanCollectionCreateRequest request = new LoanCollectionCreateRequest();
        request.setOrgId("ORG-1");
        request.setLoanId("LOAN-1");
        request.setUserId("USR-1");
        request.setCollectionAmount(new BigDecimal("1000.00"));
        return request;
    }

    private void stubParents() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        UserInfo payer = new UserInfo();
        payer.setUserId("USR-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.of(loan));
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(payer));
    }

    @Test
    @DisplayName("Create assigns a generated ID with CASH default")
    void create_AssignsId() {
        stubParents();
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("COLL-ABC123");
        when(collectionRepository.save(any(LoanCollection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanCollectionDTO created = collectionService.create(createRequest());

        assertEquals("COLL-ABC123", created.getCollectionId());
        assertEquals("CASH", created.getPaymentMode());
    }

    @Test
    @DisplayName("Create with an unknown loan fails with 404")
    void create_UnknownLoanFails() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> collectionService.create(createRequest()));
    }

    @Test
    @DisplayName("Create with an unknown collecting user fails with 404")
    void create_UnknownCollectorFails() {
        stubParents();
        when(userRepository.findById("USR-9")).thenReturn(Optional.empty());

        LoanCollectionCreateRequest request = createRequest();
        request.setCollectedByUserId("USR-9");

        assertThrows(ResourceNotFoundException.class,
                () -> collectionService.create(request));
    }

    @Test
    @DisplayName("Create with an unknown organization fails with 404")
    void create_UnknownOrgFails() {
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> collectionService.create(createRequest()));
    }

    @Test
    @DisplayName("Create with an unknown payer fails with 404")
    void create_UnknownUserFails() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.of(loan));
        when(userRepository.findById("USR-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> collectionService.create(createRequest()));
    }

    @Test
    @DisplayName("Create with a known collector links the collecting user")
    void create_WithCollectorSuccess() {
        stubParents();
        UserInfo collector = new UserInfo();
        collector.setUserId("USR-9");
        when(userRepository.findById("USR-9")).thenReturn(Optional.of(collector));
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("COLL-ABC123");
        when(collectionRepository.save(any(LoanCollection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanCollectionCreateRequest request = createRequest();
        request.setCollectedByUserId("USR-9");

        LoanCollectionDTO created = collectionService.create(request);

        assertEquals("COLL-ABC123", created.getCollectionId());
        assertEquals("USR-9", created.getCollectedByUserId());
        assertEquals("SUCCESS", created.getStatus());
        ArgumentCaptor<LoanCollection> saved = ArgumentCaptor.forClass(LoanCollection.class);
        verify(collectionRepository).save(saved.capture());
        assertEquals("COLL-ABC123", saved.getValue().getCollectionId());
        assertEquals(new BigDecimal("1000.00"), saved.getValue().getCollectionAmount());
        assertEquals("ORG-1", saved.getValue().getOrganization().getOrgId());
        assertEquals("LOAN-1", saved.getValue().getLoan().getLoanId());
        assertEquals("USR-1", saved.getValue().getUser().getUserId());
        assertEquals("USR-9", saved.getValue().getCollectedBy().getUserId());
        assertEquals("CASH", saved.getValue().getPaymentMode());
        assertEquals("SUCCESS", saved.getValue().getStatus());
    }

    @Test
    @DisplayName("Create without an explicit status keeps the SUCCESS default")
    void create_DefaultStatusSuccess() {
        stubParents();
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("COLL-ABC123");
        when(collectionRepository.save(any(LoanCollection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanCollectionDTO created = collectionService.create(createRequest());

        assertEquals("SUCCESS", created.getStatus());
        assertEquals("CASH", created.getPaymentMode());
    }

    @Test
    @DisplayName("Create propagates repository exceptions")
    void create_RepositoryThrows() {
        stubParents();
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("COLL-ABC123");
        when(collectionRepository.save(any(LoanCollection.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> collectionService.create(createRequest()));
    }

    @Test
    @DisplayName("Get by ID returns the DTO")
    void getById_ReturnsDto() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        UserInfo payer = new UserInfo();
        payer.setUserId("USR-1");
        LoanCollection collection = new LoanCollection();
        collection.setCollectionId("COLL-1");
        collection.setOrganization(org);
        collection.setLoan(loan);
        collection.setUser(payer);
        collection.setStatus("SUCCESS");
        when(collectionRepository.findById("COLL-1")).thenReturn(Optional.of(collection));

        LoanCollectionDTO dto = collectionService.getById("COLL-1");

        assertEquals("COLL-1", dto.getCollectionId());
        assertEquals("ORG-1", dto.getOrgId());
        assertEquals("LOAN-1", dto.getLoanId());
        assertEquals("USR-1", dto.getUserId());
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404")
    void getById_UnknownFails() {
        when(collectionRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> collectionService.getById("NOPE"));
    }

    @Test
    @DisplayName("List without filters queries all collections")
    void list_AllReturnsPage() {
        LoanCollection collection = new LoanCollection();
        collection.setCollectionId("COLL-1");
        when(collectionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(collection)));

        PageResponse<LoanCollectionDTO> page = collectionService.list(null, null, null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(collectionRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List with a loan filter queries by loan")
    void list_ByLoanId() {
        LoanCollection collection = new LoanCollection();
        collection.setCollectionId("COLL-1");
        when(collectionRepository.findByLoan_LoanId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(collection)));

        PageResponse<LoanCollectionDTO> page = collectionService.list("LOAN-1", null, null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(collectionRepository).findByLoan_LoanId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with an org filter queries by organization")
    void list_ByOrgId() {
        LoanCollection collection = new LoanCollection();
        collection.setCollectionId("COLL-1");
        when(collectionRepository.findByOrganization_OrgId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(collection)));

        PageResponse<LoanCollectionDTO> page = collectionService.list(null, "ORG-1", null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(collectionRepository).findByOrganization_OrgId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with a status filter queries by status")
    void list_ByStatus() {
        LoanCollection collection = new LoanCollection();
        collection.setCollectionId("COLL-1");
        when(collectionRepository.findByStatus(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(collection)));

        PageResponse<LoanCollectionDTO> page = collectionService.list(null, null, "SUCCESS", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(collectionRepository).findByStatus(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List prefers the loan filter when several filters are supplied")
    void list_LoanTakesPriority() {
        LoanCollection collection = new LoanCollection();
        collection.setCollectionId("COLL-1");
        when(collectionRepository.findByLoan_LoanId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(collection)));

        PageResponse<LoanCollectionDTO> page = collectionService.list("LOAN-1", "ORG-1", "SUCCESS", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(collectionRepository).findByLoan_LoanId(anyString(), any(Pageable.class));
        verify(collectionRepository, never()).findByOrganization_OrgId(anyString(), any(Pageable.class));
        verify(collectionRepository, never()).findByStatus(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(collectionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<LoanCollectionDTO> page = collectionService.list(null, null, null, 0, 10);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Update changes only reference, remarks, and status")
    void update_Success() {
        LoanCollection collection = new LoanCollection();
        collection.setCollectionId("COLL-1");
        collection.setCollectionAmount(new BigDecimal("1000.00"));
        collection.setStatus("SUCCESS");
        LoanCollectionUpdateRequest request = new LoanCollectionUpdateRequest();
        request.setReferenceNo("REF-99");
        request.setRemarks("Verified");
        request.setStatus("RECONCILED");
        when(collectionRepository.findById("COLL-1")).thenReturn(Optional.of(collection));
        when(collectionRepository.save(any(LoanCollection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanCollectionDTO updated = collectionService.update("COLL-1", request);

        assertEquals("REF-99", updated.getReferenceNo());
        assertEquals("Verified", updated.getRemarks());
        assertEquals("RECONCILED", updated.getStatus());
        assertEquals(new BigDecimal("1000.00"), updated.getCollectionAmount());
    }

    @Test
    @DisplayName("Update with an unknown ID fails with 404")
    void update_UnknownFails() {
        LoanCollectionUpdateRequest request = new LoanCollectionUpdateRequest();
        request.setStatus("RECONCILED");
        when(collectionRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> collectionService.update("NOPE", request));
    }

    @Test
    @DisplayName("Delete removes the collection")
    void delete_Removes() {
        LoanCollection collection = new LoanCollection();
        collection.setCollectionId("COLL-1");
        when(collectionRepository.findById("COLL-1")).thenReturn(Optional.of(collection));

        collectionService.delete("COLL-1");

        verify(collectionRepository).delete(collection);
    }

    @Test
    @DisplayName("Delete with an unknown ID fails with 404")
    void delete_UnknownFails() {
        when(collectionRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> collectionService.delete("NOPE"));
    }
}
