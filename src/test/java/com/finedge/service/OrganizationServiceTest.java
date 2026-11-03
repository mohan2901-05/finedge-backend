package com.finedge.service;

import com.finedge.dto.OrganizationCreateRequest;
import com.finedge.dto.OrganizationDTO;
import com.finedge.dto.OrganizationUpdateRequest;
import com.finedge.dto.PageResponse;
import com.finedge.entity.Organization;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @InjectMocks
    private OrganizationService organizationService;

    private OrganizationCreateRequest createRequest() {
        OrganizationCreateRequest request = new OrganizationCreateRequest();
        request.setOrgName("Test Org");
        request.setOrgEmail("test@example.invalid");
        return request;
    }

    @Test
    @DisplayName("Create assigns a generated ID and returns the DTO")
    void create_AssignsId() {
        when(organizationRepository.existsByOrgEmail("test@example.invalid")).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("ORG-ABC123");
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationDTO created = organizationService.create(createRequest());

        assertEquals("ORG-ABC123", created.getOrgId());
        assertEquals("Test Org", created.getOrgName());
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("Create with a duplicate email fails with 409 semantics")
    void create_DuplicateEmailFails() {
        when(organizationRepository.existsByOrgEmail("test@example.invalid")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> organizationService.create(createRequest()));
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404 semantics")
    void getById_UnknownFails() {
        when(organizationRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> organizationService.getById("NOPE"));
    }

    @Test
    @DisplayName("Delete removes the organization")
    void delete_Removes() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));

        organizationService.delete("ORG-1");

        verify(organizationRepository).delete(org);
    }

    @Test
    @DisplayName("Create trims fields and defaults status to ACTIVE")
    void create_TrimsAndDefaultsStatus() {
        OrganizationCreateRequest request = new OrganizationCreateRequest();
        request.setOrgName("  Test Org  ");
        request.setOrgEmail("  test@example.invalid  ");
        when(organizationRepository.existsByOrgEmail("  test@example.invalid  ")).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("ORG-ABC123");
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationDTO created = organizationService.create(request);

        assertEquals("Test Org", created.getOrgName());
        assertEquals("test@example.invalid", created.getOrgEmail());
        assertEquals("ACTIVE", created.getStatus());
    }

    @Test
    @DisplayName("Create propagates repository exceptions")
    void create_RepositoryThrows() {
        when(organizationRepository.existsByOrgEmail("test@example.invalid")).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("ORG-ABC123");
        when(organizationRepository.save(any(Organization.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> organizationService.create(createRequest()));
    }

    @Test
    @DisplayName("Get by ID returns the DTO")
    void getById_ReturnsDto() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        org.setOrgName("Test Org");
        org.setOrgEmail("test@example.invalid");
        org.setStatus("ACTIVE");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));

        OrganizationDTO dto = organizationService.getById("ORG-1");

        assertEquals("ORG-1", dto.getOrgId());
        assertEquals("Test Org", dto.getOrgName());
    }

    @Test
    @DisplayName("List without a filter queries all organizations")
    void list_AllReturnsPage() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        when(organizationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(org)));

        PageResponse<OrganizationDTO> page = organizationService.list(null, 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("ORG-1", page.getContent().get(0).getOrgId());
        verify(organizationRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List with a status filter queries by status")
    void list_ByStatus() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        when(organizationRepository.findByStatus(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(org)));

        PageResponse<OrganizationDTO> page = organizationService.list("ACTIVE", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(organizationRepository).findByStatus(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(organizationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<OrganizationDTO> page = organizationService.list(null, 0, 10);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Update changes mutable fields")
    void update_Success() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        org.setOrgName("Old");
        org.setOrgEmail("old@example.invalid");
        org.setStatus("ACTIVE");
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setOrgName(" New Name ");
        request.setStatus("INACTIVE");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationDTO updated = organizationService.update("ORG-1", request);

        assertEquals("New Name", updated.getOrgName());
        assertEquals("INACTIVE", updated.getStatus());
        assertEquals("old@example.invalid", updated.getOrgEmail());
    }

    @Test
    @DisplayName("Update to a taken email fails")
    void update_DuplicateEmailFails() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        org.setOrgName("Old");
        org.setOrgEmail("old@example.invalid");
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setOrgEmail("taken@example.invalid");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(organizationRepository.existsByOrgEmail("taken@example.invalid")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> organizationService.update("ORG-1", request));
    }

    @Test
    @DisplayName("Update keeping the same email (case-insensitive) succeeds")
    void update_SameEmailAllows() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        org.setOrgName("Old");
        org.setOrgEmail("old@example.invalid");
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setOrgEmail("OLD@example.invalid");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        // Even if the email were taken elsewhere, the unchanged (case-insensitive)
        // email must not trigger the duplicate guard: the lenient stub only takes
        // effect if the implementation wrongly consults the repository.
        lenient().when(organizationRepository.existsByOrgEmail("OLD@example.invalid")).thenReturn(true);
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationDTO updated = organizationService.update("ORG-1", request);

        assertEquals("OLD@example.invalid", updated.getOrgEmail());
        verify(organizationRepository).save(any(Organization.class));
        verify(organizationRepository, never()).existsByOrgEmail(anyString());
    }

    @Test
    @DisplayName("Update with an unknown ID fails with 404")
    void update_UnknownFails() {
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setOrgName("New");
        when(organizationRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> organizationService.update("NOPE", request));
    }

    @Test
    @DisplayName("Delete with an unknown ID fails with 404")
    void delete_UnknownFails() {
        when(organizationRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> organizationService.delete("NOPE"));
    }
}
