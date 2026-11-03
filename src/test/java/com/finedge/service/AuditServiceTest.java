package com.finedge.service;

import com.finedge.dto.AuditLogCreateRequest;
import com.finedge.dto.AuditLogDTO;
import com.finedge.dto.PageResponse;
import com.finedge.entity.AuditLog;
import com.finedge.entity.Organization;
import com.finedge.entity.UserInfo;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.AuditLogRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserInfoRepository userRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @InjectMocks
    private AuditService auditService;

    private AuditLogCreateRequest createRequest() {
        AuditLogCreateRequest request = new AuditLogCreateRequest();
        request.setOrgId("ORG-1");
        request.setUserId("USR-1");
        request.setUserName(" Test User ");
        request.setAction(" CREATE_LOAN ");
        request.setDetails("Created loan LOAN-1");
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

    private AuditLog existingAudit() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        AuditLog audit = new AuditLog();
        audit.setAuditId("AUD-1");
        audit.setOrganization(org);
        audit.setUser(user);
        audit.setUserName("Test User");
        audit.setAction("CREATE_LOAN");
        audit.setDetails("Created loan LOAN-1");
        return audit;
    }

    @Test
    @DisplayName("Create assigns a generated ID and trims user name/action")
    void create_AssignsId() {
        stubParents();
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("AUD-ABC123");
        when(auditRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLogDTO created = auditService.create(createRequest());

        assertEquals("AUD-ABC123", created.getAuditId());
        assertEquals("Test User", created.getUserName());
        assertEquals("CREATE_LOAN", created.getAction());
        assertEquals("ORG-1", created.getOrgId());
        assertEquals("USR-1", created.getUserId());
        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepository).save(saved.capture());
        assertEquals("AUD-ABC123", saved.getValue().getAuditId());
        assertEquals("Test User", saved.getValue().getUserName());
        assertEquals("CREATE_LOAN", saved.getValue().getAction());
        assertEquals("Created loan LOAN-1", saved.getValue().getDetails());
        assertEquals("ORG-1", saved.getValue().getOrganization().getOrgId());
        assertEquals("USR-1", saved.getValue().getUser().getUserId());
    }

    @Test
    @DisplayName("Create with an unknown organization fails with 404")
    void create_UnknownOrgFails() {
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> auditService.create(createRequest()));
    }

    @Test
    @DisplayName("Create with an unknown user fails with 404")
    void create_UnknownUserFails() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(userRepository.findById("USR-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> auditService.create(createRequest()));
    }

    @Test
    @DisplayName("Create propagates repository exceptions")
    void create_RepositoryThrows() {
        stubParents();
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("AUD-ABC123");
        when(auditRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> auditService.create(createRequest()));
    }

    @Test
    @DisplayName("Get by ID returns the DTO")
    void getById_ReturnsDto() {
        when(auditRepository.findById("AUD-1")).thenReturn(Optional.of(existingAudit()));

        AuditLogDTO dto = auditService.getById("AUD-1");

        assertEquals("AUD-1", dto.getAuditId());
        assertEquals("ORG-1", dto.getOrgId());
        assertEquals("USR-1", dto.getUserId());
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404")
    void getById_UnknownFails() {
        when(auditRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> auditService.getById("NOPE"));
    }

    @Test
    @DisplayName("List without filters queries all entries")
    void list_AllReturnsPage() {
        AuditLog audit = existingAudit();
        when(auditRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(audit)));

        PageResponse<AuditLogDTO> page = auditService.list(null, null, 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("AUD-1", page.getContent().get(0).getAuditId());
        verify(auditRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List with an org filter queries by organization")
    void list_ByOrgId() {
        AuditLog audit = existingAudit();
        when(auditRepository.findByOrganization_OrgIdOrderByCreatedDatetimeDesc(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(audit)));

        PageResponse<AuditLogDTO> page = auditService.list("ORG-1", null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(auditRepository).findByOrganization_OrgIdOrderByCreatedDatetimeDesc(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with a user filter queries by user")
    void list_ByUserId() {
        AuditLog audit = existingAudit();
        when(auditRepository.findByUser_UserIdOrderByCreatedDatetimeDesc(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(audit)));

        PageResponse<AuditLogDTO> page = auditService.list(null, "USR-1", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(auditRepository).findByUser_UserIdOrderByCreatedDatetimeDesc(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List prefers the org filter when both filters are supplied")
    void list_OrgTakesPriorityOverUser() {
        AuditLog audit = existingAudit();
        when(auditRepository.findByOrganization_OrgIdOrderByCreatedDatetimeDesc(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(audit)));

        PageResponse<AuditLogDTO> page = auditService.list("ORG-1", "USR-1", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(auditRepository).findByOrganization_OrgIdOrderByCreatedDatetimeDesc(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(auditRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<AuditLogDTO> page = auditService.list(null, null, 0, 10);

        assertTrue(page.getContent().isEmpty());
    }
}
