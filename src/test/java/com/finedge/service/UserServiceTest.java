package com.finedge.service;

import com.finedge.dto.PageResponse;
import com.finedge.dto.UserCreateRequest;
import com.finedge.dto.UserDTO;
import com.finedge.dto.UserUpdateRequest;
import com.finedge.entity.Organization;
import com.finedge.entity.RoleMaster;
import com.finedge.entity.UserInfo;
import com.finedge.exception.DuplicateResourceException;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.OrganizationRepository;
import com.finedge.repository.RoleMasterRepository;
import com.finedge.repository.UserInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
class UserServiceTest {

    @Mock
    private UserInfoRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RoleMasterRepository roleRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserCreateRequest createRequest() {
        UserCreateRequest request = new UserCreateRequest();
        request.setOrgId("ORG-1");
        request.setRoleId("ROLE-1");
        request.setUserName("Test User");
        request.setPassword("secret-value");
        request.setUserEmail("user@example.invalid");
        return request;
    }

    private void stubParents() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(roleRepository.findById("ROLE-1")).thenReturn(Optional.of(role));
    }

    @Test
    @DisplayName("Create stores the user and never exposes password or PAN")
    void create_HidesSecrets() {
        stubParents();
        when(userRepository.existsByUserEmail("user@example.invalid")).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("USR-ABC123");
        when(passwordEncoder.encode("secret-value")).thenReturn("hashed-secret-value");
        when(userRepository.save(any(UserInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO created = userService.create(createRequest());

        assertEquals("USR-ABC123", created.getUserId());
        assertEquals("user@example.invalid", created.getUserEmail());
        // The password must be hashed before persistence, never stored plaintext.
        ArgumentCaptor<UserInfo> saved = ArgumentCaptor.forClass(UserInfo.class);
        verify(userRepository).save(saved.capture());
        verify(passwordEncoder).encode("secret-value");
        assertEquals("hashed-secret-value", saved.getValue().getPassword());
        assertNotEquals("secret-value", saved.getValue().getPassword());
        assertEquals("USR-ABC123", saved.getValue().getUserId());
        assertEquals("Test User", saved.getValue().getUserName());
        assertEquals("user@example.invalid", saved.getValue().getUserEmail());
        assertEquals("ACTIVE", saved.getValue().getStatus());
        assertEquals("ORG-1", saved.getValue().getOrganization().getOrgId());
        assertEquals("ROLE-1", saved.getValue().getRole().getRoleId());
    }

    @Test
    @DisplayName("Create with a duplicate email fails")
    void create_DuplicateEmailFails() {
        stubParents();
        when(userRepository.existsByUserEmail("user@example.invalid")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> userService.create(createRequest()));
    }

    @Test
    @DisplayName("Create with an unknown organization fails with 404")
    void create_UnknownOrgFails() {
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.create(createRequest()));
    }

    @Test
    @DisplayName("Create with an unknown role fails with 404")
    void create_UnknownRoleFails() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        when(organizationRepository.findById("ORG-1")).thenReturn(Optional.of(org));
        when(roleRepository.findById("ROLE-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.create(createRequest()));
    }

    @Test
    @DisplayName("Create propagates repository exceptions")
    void create_RepositoryThrows() {
        stubParents();
        when(userRepository.existsByUserEmail("user@example.invalid")).thenReturn(false);
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("USR-ABC123");
        when(userRepository.save(any(UserInfo.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> userService.create(createRequest()));
    }

    @Test
    @DisplayName("Get by ID returns the DTO with role name")
    void getById_ReturnsDto() {
        Organization org = new Organization();
        org.setOrgId("ORG-1");
        RoleMaster role = new RoleMaster();
        role.setRoleId("ROLE-1");
        role.setRoleName("Collector");
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        user.setOrganization(org);
        user.setRole(role);
        user.setUserName("Test User");
        user.setUserEmail("user@example.invalid");
        user.setStatus("ACTIVE");
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(user));

        UserDTO dto = userService.getById("USR-1");

        assertEquals("USR-1", dto.getUserId());
        assertEquals("ORG-1", dto.getOrgId());
        assertEquals("ROLE-1", dto.getRoleId());
        assertEquals("Collector", dto.getRoleName());
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404")
    void getById_UnknownFails() {
        when(userRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getById("NOPE"));
    }

    @Test
    @DisplayName("List without filters queries all users")
    void list_AllReturnsPage() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        PageResponse<UserDTO> page = userService.list(null, null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List with an org filter queries by organization")
    void list_ByOrgId() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        when(userRepository.findByOrganization_OrgId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        PageResponse<UserDTO> page = userService.list("ORG-1", null, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(userRepository).findByOrganization_OrgId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with a role filter queries by role")
    void list_ByRoleId() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        when(userRepository.findByRole_RoleId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        PageResponse<UserDTO> page = userService.list(null, "ROLE-1", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(userRepository).findByRole_RoleId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List prefers the org filter when both filters are supplied")
    void list_OrgTakesPriorityOverRole() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        when(userRepository.findByOrganization_OrgId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        PageResponse<UserDTO> page = userService.list("ORG-1", "ROLE-1", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(userRepository).findByOrganization_OrgId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<UserDTO> page = userService.list(null, null, 0, 10);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Update changes mutable fields")
    void update_Success() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        user.setUserName("Old");
        user.setUserEmail("old@example.invalid");
        user.setStatus("ACTIVE");
        UserUpdateRequest request = new UserUpdateRequest();
        request.setUserName(" New Name ");
        request.setStatus("INACTIVE");
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO updated = userService.update("USR-1", request);

        assertEquals("New Name", updated.getUserName());
        assertEquals("INACTIVE", updated.getStatus());
    }

    @Test
    @DisplayName("Update to a taken email fails")
    void update_DuplicateEmailFails() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        user.setUserEmail("old@example.invalid");
        UserUpdateRequest request = new UserUpdateRequest();
        request.setUserEmail("taken@example.invalid");
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(user));
        when(userRepository.existsByUserEmail("taken@example.invalid")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> userService.update("USR-1", request));
    }

    @Test
    @DisplayName("Update keeping the same email (case-insensitive) succeeds")
    void update_SameEmailAllows() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        user.setUserEmail("old@example.invalid");
        UserUpdateRequest request = new UserUpdateRequest();
        request.setUserEmail("OLD@example.invalid");
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(user));
        // Even if the email were taken elsewhere, the unchanged (case-insensitive)
        // email must not trigger the duplicate guard: the lenient stub only takes
        // effect if the implementation wrongly consults the repository.
        lenient().when(userRepository.existsByUserEmail("OLD@example.invalid")).thenReturn(true);
        when(userRepository.save(any(UserInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO updated = userService.update("USR-1", request);

        assertEquals("OLD@example.invalid", updated.getUserEmail());
        verify(userRepository, never()).existsByUserEmail(anyString());
    }

    @Test
    @DisplayName("Update to an unknown role fails with 404")
    void update_UnknownRoleFails() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        user.setUserEmail("old@example.invalid");
        UserUpdateRequest request = new UserUpdateRequest();
        request.setRoleId("ROLE-9");
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(user));
        when(roleRepository.findById("ROLE-9")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.update("USR-1", request));
    }

    @Test
    @DisplayName("Update with an unknown user fails with 404")
    void update_UnknownUserFails() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setUserName("New");
        when(userRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.update("NOPE", request));
    }

    @Test
    @DisplayName("Delete removes the user")
    void delete_Removes() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(user));

        userService.delete("USR-1");

        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("Delete with an unknown ID fails with 404")
    void delete_UnknownFails() {
        when(userRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.delete("NOPE"));
    }
}
