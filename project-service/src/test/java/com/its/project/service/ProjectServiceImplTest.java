package com.its.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.its.project.client.IssueGateway;
import com.its.project.client.UserGateway;
import com.its.project.dto.request.ProjectRequest;
import com.its.project.dto.response.ProjectResponse;
import com.its.project.dto.response.UserSummary;
import com.its.project.entity.Project;
import com.its.project.exception.ProjectExceptions.DuplicateResourceException;
import com.its.project.exception.ProjectExceptions.InvalidReferenceException;
import com.its.project.exception.ProjectExceptions.ServiceUnavailableException;
import com.its.project.repository.ProjectRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserGateway userGateway;

    @Mock
    private IssueGateway issueGateway;

    private ProjectServiceImpl service() {
        return new ProjectServiceImpl(projectRepository, userGateway, issueGateway);
    }

    private static final UserSummary OWNER = new UserSummary(
            101, "Emily Sinha", "emily.sinha@example.com", "emily.sinha", "PROJECT_OWNER");

    private static final UserSummary ASSIGNEE = new UserSummary(
            104, "Carlos Singh", "carlos.singh@example.com", "carlos.singh", "ASSIGNEE");

    private ProjectRequest request(Integer ownerId, LocalDate start, LocalDate end) {
        return new ProjectRequest("Profile Management", ownerId, start, end);
    }

    @Test
    @DisplayName("Creates a project once the owner is confirmed")
    void createsProject() {
        when(userGateway.findById(101)).thenReturn(Optional.of(OWNER));
        when(projectRepository.existsByProjectName(anyString())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse response = service().create(
                request(101, LocalDate.of(2025, 9, 18), LocalDate.of(2025, 12, 18)));

        assertThat(response.projectName()).isEqualTo("Profile Management");
        assertThat(response.projectOwnerId()).isEqualTo(101);
    }

    @Test
    @DisplayName("An owner id that does not exist is a 400, not a 404")
    void rejectsUnknownOwner() {
        when(userGateway.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(
                request(999, LocalDate.now(), LocalDate.now().plusDays(30))))
                .isInstanceOf(InvalidReferenceException.class)
                .hasMessageContaining("No user exists with id 999");

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("An Assignee cannot be recorded as a project owner")
    void rejectsNonOwnerRole() {
        when(userGateway.findById(104)).thenReturn(Optional.of(ASSIGNEE));

        // Without this check an Assignee ends up owning a project they can never
        // administer, because every role gate downstream will refuse them.
        assertThatThrownBy(() -> service().create(
                request(104, LocalDate.now(), LocalDate.now().plusDays(30))))
                .isInstanceOf(InvalidReferenceException.class)
                .hasMessageContaining("is not a Project Owner");

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("An end date before the start date is rejected")
    void rejectsInvertedDates() {
        assertThatThrownBy(() -> service().create(
                request(101, LocalDate.of(2025, 12, 18), LocalDate.of(2025, 9, 18))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate must not be earlier than startDate");

        // Validation happens before the User Service is troubled.
        verify(userGateway, never()).findById(any());
    }

    @Test
    @DisplayName("A null end date is allowed - only ordering is constrained")
    void allowsOpenEndedProject() {
        when(userGateway.findById(101)).thenReturn(Optional.of(OWNER));
        when(projectRepository.existsByProjectName(anyString())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse response = service().create(
                request(101, LocalDate.of(2025, 9, 18), null));

        assertThat(response.endDate()).isNull();
    }

    @Test
    @DisplayName("A duplicate project name is a 409 against that field")
    void rejectsDuplicateName() {
        when(userGateway.findById(101)).thenReturn(Optional.of(OWNER));
        when(projectRepository.existsByProjectName("Profile Management")).thenReturn(true);

        assertThatThrownBy(() -> service().create(
                request(101, LocalDate.now(), LocalDate.now().plusDays(10))))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting("field").isEqualTo("projectName");
    }

    @Test
    @DisplayName("A User Service outage during create is a 503, not a bad-reference 400")
    void userServiceOutageIsNotMistakenForBadInput() {
        when(userGateway.findById(101)).thenThrow(
                new ServiceUnavailableException("User Service", new RuntimeException()));

        // Reporting "no such owner" here would blame the caller for our outage, and
        // would have them correcting a request that was always valid.
        assertThatThrownBy(() -> service().create(
                request(101, LocalDate.now(), LocalDate.now().plusDays(10))))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
