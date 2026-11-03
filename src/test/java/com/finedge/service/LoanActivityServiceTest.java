package com.finedge.service;

import com.finedge.dto.LoanActivityStatusCreateRequest;
import com.finedge.dto.LoanActivityStatusDTO;
import com.finedge.dto.PageResponse;
import com.finedge.entity.LoanActivityStatus;
import com.finedge.entity.LoanInfo;
import com.finedge.entity.UserInfo;
import com.finedge.exception.ResourceNotFoundException;
import com.finedge.repository.LoanActivityStatusRepository;
import com.finedge.repository.LoanInfoRepository;
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
class LoanActivityServiceTest {

    @Mock
    private LoanActivityStatusRepository activityRepository;

    @Mock
    private LoanInfoRepository loanRepository;

    @Mock
    private UserInfoRepository userRepository;

    @Mock
    private StringIdGenerator idGenerator;

    @InjectMocks
    private LoanActivityService activityService;

    private LoanActivityStatusCreateRequest createRequest() {
        LoanActivityStatusCreateRequest request = new LoanActivityStatusCreateRequest();
        request.setUserId("USR-1");
        request.setLoanId("LOAN-1");
        request.setActivityCode(" BDE ");
        request.setStatusCode(" A ");
        return request;
    }

    private void stubParents() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(user));
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.of(loan));
    }

    private LoanActivityStatus existingActivity() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        LoanInfo loan = new LoanInfo();
        loan.setLoanId("LOAN-1");
        LoanActivityStatus activity = new LoanActivityStatus();
        activity.setActivityId("LAS-1");
        activity.setUser(user);
        activity.setLoan(loan);
        activity.setActivityCode("BDE");
        activity.setStatusCode("A");
        return activity;
    }

    @Test
    @DisplayName("Create assigns a generated ID and trims codes")
    void create_AssignsId() {
        stubParents();
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("LAS-ABC123");
        when(activityRepository.save(any(LoanActivityStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanActivityStatusDTO created = activityService.create(createRequest());

        assertEquals("LAS-ABC123", created.getActivityId());
        assertEquals("BDE", created.getActivityCode());
        assertEquals("A", created.getStatusCode());
        assertEquals("USR-1", created.getUserId());
        assertEquals("LOAN-1", created.getLoanId());
        ArgumentCaptor<LoanActivityStatus> saved = ArgumentCaptor.forClass(LoanActivityStatus.class);
        verify(activityRepository).save(saved.capture());
        assertEquals("LAS-ABC123", saved.getValue().getActivityId());
        assertEquals("BDE", saved.getValue().getActivityCode());
        assertEquals("A", saved.getValue().getStatusCode());
        assertEquals("USR-1", saved.getValue().getUser().getUserId());
        assertEquals("LOAN-1", saved.getValue().getLoan().getLoanId());
    }

    @Test
    @DisplayName("Create with an unknown user fails with 404")
    void create_UnknownUserFails() {
        when(userRepository.findById("USR-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> activityService.create(createRequest()));
    }

    @Test
    @DisplayName("Create with an unknown loan fails with 404")
    void create_UnknownLoanFails() {
        UserInfo user = new UserInfo();
        user.setUserId("USR-1");
        when(userRepository.findById("USR-1")).thenReturn(Optional.of(user));
        when(loanRepository.findById("LOAN-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> activityService.create(createRequest()));
    }

    @Test
    @DisplayName("Create propagates repository exceptions")
    void create_RepositoryThrows() {
        stubParents();
        when(idGenerator.generateUnique(anyString(), ArgumentMatchers.<Predicate<String>>any())).thenReturn("LAS-ABC123");
        when(activityRepository.save(any(LoanActivityStatus.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class,
                () -> activityService.create(createRequest()));
    }

    @Test
    @DisplayName("Get by ID returns the DTO")
    void getById_ReturnsDto() {
        when(activityRepository.findById("LAS-1")).thenReturn(Optional.of(existingActivity()));

        LoanActivityStatusDTO dto = activityService.getById("LAS-1");

        assertEquals("LAS-1", dto.getActivityId());
        assertEquals("LOAN-1", dto.getLoanId());
        assertEquals("USR-1", dto.getUserId());
    }

    @Test
    @DisplayName("Get with an unknown ID fails with 404")
    void getById_UnknownFails() {
        when(activityRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> activityService.getById("NOPE"));
    }

    @Test
    @DisplayName("List without a filter queries all activities")
    void list_AllReturnsPage() {
        LoanActivityStatus activity = existingActivity();
        when(activityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activity)));

        PageResponse<LoanActivityStatusDTO> page = activityService.list(null, 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("LAS-1", page.getContent().get(0).getActivityId());
        verify(activityRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("List with a loan filter queries by loan")
    void list_ByLoanId() {
        LoanActivityStatus activity = existingActivity();
        when(activityRepository.findByLoan_LoanId(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activity)));

        PageResponse<LoanActivityStatusDTO> page = activityService.list("LOAN-1", 0, 10);

        assertEquals(1, page.getContent().size());
        verify(activityRepository).findByLoan_LoanId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("List with no rows returns an empty page")
    void list_EmptyReturnsEmpty() {
        when(activityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        PageResponse<LoanActivityStatusDTO> page = activityService.list(null, 0, 10);

        assertTrue(page.getContent().isEmpty());
    }
}
