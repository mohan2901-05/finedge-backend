package com.finedge.service;

import com.finedge.dto.LoanInfoCreateRequest;
import com.finedge.dto.LoanInfoDTO;
import com.finedge.dto.LoanInfoUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.LoanInfo;
import com.finedge.entity.Organization;
import com.finedge.entity.UserInfo;
import com.finedge.exception.ResourceNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanInfoServiceTest {

    @Mock
    private LoanInfoRepository loanRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserInfoRepository userRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @InjectMocks
    private LoanInfoService loanService;

    private LoanInfoCreateRequest createRequest() {
        LoanInfoCreateRequest request = new LoanInfoCreateRequest();
        request.setOrgId("ORG-1");
        request.setUserId("USR-1");
        request.setLoanAmount(new BigDecimal("120000.00"));
        request.setTenureMonths(12);
        return request;
    }

    private void stubParents() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("Create assigns a generated ID with PENDING default")
    void create_AssignsId() {
        stubParents();
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("LOAN-ABC123");
        when(loanRepository.save(any(LoanInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanInfoDTO created = loanService.create(createRequest());

        assertEquals("LOAN-ABC123", created.getLoanId());
        assertEquals("PENDING", created.getStatus());
        ArgumentCaptor<LoanInfo> saved = ArgumentCaptor.forClass(LoanInfo.class);
        verify(loanRepository).save(saved.capture());
        assertEquals("LOAN-ABC123", saved.getValue().getLoanId());
        assertEquals(new BigDecimal("120000.00"), saved.getValue().getLoanAmount());
        assertEquals(Integer.valueOf(12), saved.getValue().getTenureMonths());
        assertEquals("ORG-1", saved.getValue().getOrganization().getOrgId());
        assertEquals("USR-1", saved.getValue().getUser().getUserId());
    }

    @Test
    @DisplayName("Received flag maps between Boolean DTO and numeric storage")
    void create_ReceivedMapping() {
        stubParents();
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("LOAN-ABC123");
        when(loanRepository.save(any(LoanInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanInfoCreateRequest request = createRequest();
        request.setReceived(true);

        assertTrue(loanService.create(request).getReceived());
    }

    @Test
    @DisplayName("Create with an unknown organization fails with 404")
    void create_UnknownOrgFails() {
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> loanService.create(createRequest()));
    }

    @Test
    @DisplayName("Create with an unknown user fails with 404")
    void create_UnknownUserFails() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(userRepository.findById("USR-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> loanService.create(createRequest()));
    }

    @Test
    @DisplayName("Create without optional status/received keeps PENDING/false defaults")
    void create_DefaultsWhenNull() {
        stubParents();
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("LOAN-ABC123");
        when(loanRepository.save(any(LoanInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanInfoDTO created = loanService.create(createRequest());

        assertEquals("PENDING", created.getStatus());
        assertFalse(created.getReceived());
    }

    @Test
    @DisplayName("Create propagates repository exceptions")
    void create_RepositoryThrows() {
        stubParents();
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("LOAN-ABC123");
        when(loanRepository.save(any(LoanInfo.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> loanService.create(createRequest()));
    }

    @Test
    @DisplayName("Get by ID returns the DTO")
    void getById_ReturnsDto() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        loan.setOrganization(org);
        loan.setUser(user);
        loan.setStatus("PENDING");
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.of(loan));

        LoanInfoDTO dto = loanService.getById("LOAN-1");

        assertEquals("LOAN-1", dto.getLoanId());
        assertEquals("ORG-1", dto.getOrgId());
        assertEquals("USR-1", dto.getUserId());
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404")
    void getById_UnknownFails() {
        when(loanRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> loanService.getById("NOPE"));
    }

    @Test
    @DisplayName("List without filters queries all loans")
    void list_AllReturnsPage() {
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        when(loanRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan)));

        PageResponse<LoanInfoDTO> page = loanService.list(null, null, null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(loanRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List with an org filter queries by organization")
    void list_ByOrgId() {
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        when(loanRepository.findByOrganization_OrgId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan)));

        PageResponse<LoanInfoDTO> page = loanService.list("ORG-1", null, null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(loanRepository).findByOrganization_OrgId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with a user filter queries by user")
    void list_ByUserId() {
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        when(loanRepository.findByUser_UserId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan)));

        PageResponse<LoanInfoDTO> page = loanService.list(null, "USR-1", null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(loanRepository).findByUser_UserId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with a status filter queries by status")
    void list_ByStatus() {
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        when(loanRepository.findByStatus(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan)));

        PageResponse<LoanInfoDTO> page = loanService.list(null, null, "PENDING", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(loanRepository).findByStatus(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List prefers the org filter when several filters are supplied")
    void list_OrgTakesPriority() {
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        when(loanRepository.findByOrganization_OrgId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan)));

        PageResponse<LoanInfoDTO> page = loanService.list("ORG-1", "USR-1", "PENDING", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(loanRepository).findByOrganization_OrgId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(loanRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<LoanInfoDTO> page = loanService.list(null, null, null, 0, 10);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Update changes only the mutable operational fields")
    void update_Success() {
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        loan.setLoanAmount(new BigDecimal("120000.00"));
        loan.setTenureMonths(12);
        loan.setStatus("PENDING");
        LoanInfoUpdateRequest request = new LoanInfoUpdateRequest();
        request.setPurpose("Home renovation");
        request.setStatus("APPROVED");
        request.setReceived(true);
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(LoanInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanInfoDTO updated = loanService.update("LOAN-1", request);

        assertEquals("Home renovation", updated.getPurpose());
        assertEquals("APPROVED", updated.getStatus());
        assertTrue(updated.getReceived());
        assertEquals(new BigDecimal("120000.00"), updated.getLoanAmount());
    }

    @Test
    @DisplayName("Update with an unknown ID fails with 404")
    void update_UnknownFails() {
        LoanInfoUpdateRequest request = new LoanInfoUpdateRequest();
        request.setStatus("APPROVED");
        when(loanRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> loanService.update("NOPE", request));
    }

    @Test
    @DisplayName("Delete removes the loan")
    void delete_Removes() {
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.of(loan));

        loanService.delete("LOAN-1");

        verify(loanRepository).delete(loan);
    }

    @Test
    @DisplayName("Delete with an unknown ID fails with 404")
    void delete_UnknownFails() {
        when(loanRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> loanService.delete("NOPE"));
    }
}
