package com.its.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.its.project.client.IssueGateway;
import com.its.project.client.UserGateway;
import com.its.project.entity.Project;
import com.its.project.exception.ProjectExceptions.ResourceNotFoundException;
import com.its.project.exception.ProjectExceptions.ServiceUnavailableException;
import com.its.project.repository.ProjectRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The cascade delete is the one operation in this system with an irreversible failure
 * mode, so its ordering is pinned by tests rather than left to a comment.
 *
 * <p>The property under test is not "the delete works" - it is "a delete that fails
 * part-way leaves the data reachable". A cascade that only ever gets exercised on the
 * happy path proves nothing about FR-PRJ-11.
 */
@ExtendWith(MockitoExtension.class)
class ProjectCascadeDeleteTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserGateway userGateway;

    @Mock
    private IssueGateway issueGateway;

    private ProjectServiceImpl service() {
        return new ProjectServiceImpl(projectRepository, userGateway, issueGateway);
    }

    private Project project(int id) {
        Project project = new Project("Profile Management", 101,
                LocalDate.of(2025, 9, 18), LocalDate.of(2025, 12, 18));
        ReflectionTestUtils.setField(project, "projectId", id);
        return project;
    }

    @Test
    @DisplayName("Issues are deleted before the project row, never after")
    void deletesChildrenBeforeParent() {
        Project project = project(1011);
        when(projectRepository.findById(1011)).thenReturn(Optional.of(project));

        service().delete(1011);

        // Ordering is the safety property: reversing these two lines is what creates
        // orphaned issues that nothing can ever reach or clean up.
        InOrder order = inOrder(issueGateway, projectRepository);
        order.verify(issueGateway).deleteByProject(1011);
        order.verify(projectRepository).delete(project);
    }

    @Test
    @DisplayName("A failed cascade leaves the project row intact so the call can be retried")
    void failedCascadeLeavesProjectIntact() {
        Project project = project(1011);
        when(projectRepository.findById(1011)).thenReturn(Optional.of(project));
        doThrow(new ServiceUnavailableException("Issue Service", new RuntimeException("boom")))
                .when(issueGateway).deleteByProject(1011);

        assertThatThrownBy(() -> service().delete(1011))
                .isInstanceOf(ServiceUnavailableException.class);

        // The whole point. Had the project been deleted first, its issues would now be
        // unreachable garbage with nothing left to trigger a retry.
        verify(projectRepository, never()).delete(any(Project.class));
    }

    @Test
    @DisplayName("Deleting an unknown project is a 404 and touches nothing downstream")
    void unknownProjectDoesNotCascade() {
        when(projectRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(9999))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(issueGateway, never()).deleteByProject(any());
        verify(projectRepository, never()).delete(any(Project.class));
    }

    @Test
    @DisplayName("A project with no issues still deletes cleanly")
    void childlessProjectDeletes() {
        Project project = project(1013);
        when(projectRepository.findById(1013)).thenReturn(Optional.of(project));

        service().delete(1013);

        // Project 1013 in the workbook has no issues; the cascade is still invoked, and
        // the Issue Service is responsible for making a no-op delete succeed.
        verify(issueGateway).deleteByProject(1013);
        verify(projectRepository).delete(project);
        assertThat(project.getProjectId()).isEqualTo(1013);
    }
}
