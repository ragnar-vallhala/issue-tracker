package com.its.issue.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.its.issue.client.CommentGateway;
import com.its.issue.client.ReferenceGateway;
import com.its.issue.entity.Issue;
import com.its.issue.entity.IssueType;
import com.its.issue.entity.Priority;
import com.its.issue.entity.Status;
import com.its.issue.exception.IssueExceptions.ServiceUnavailableException;
import com.its.issue.repository.IssueRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The middle and deepest steps of the delete cascade (DESIGN 6.4).
 *
 * <p>Same property as the Project Service's cascade tests: children go first, and a
 * failure part-way must leave the parent reachable so the operation can be repeated.
 */
@ExtendWith(MockitoExtension.class)
class IssueCascadeDeleteTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ReferenceGateway referenceGateway;

    @Mock
    private CommentGateway commentGateway;

    private IssueServiceImpl service() {
        return new IssueServiceImpl(issueRepository, referenceGateway, commentGateway);
    }

    private Issue issue(int id) {
        Issue issue = new Issue("Profile cache not updating", "desc", 1011, 104, 101,
                Status.TO_DO, Priority.HIGH, IssueType.BUG, 2, "Sprint 42", "profile");
        ReflectionTestUtils.setField(issue, "issueId", id);
        return issue;
    }

    @Test
    @DisplayName("Deleting an issue removes its comments first")
    void deletesCommentsBeforeIssue() {
        Issue issue = issue(1);
        when(issueRepository.findById(1)).thenReturn(Optional.of(issue));

        service().delete(1);

        InOrder order = inOrder(commentGateway, issueRepository);
        order.verify(commentGateway).deleteByIssue(1);
        order.verify(issueRepository).delete(issue);
    }

    @Test
    @DisplayName("A failed comment delete leaves the issue intact")
    void failedCommentDeleteLeavesIssue() {
        Issue issue = issue(1);
        when(issueRepository.findById(1)).thenReturn(Optional.of(issue));
        doThrow(new ServiceUnavailableException("Comment Service", new RuntimeException()))
                .when(commentGateway).deleteByIssue(1);

        assertThatThrownBy(() -> service().delete(1))
                .isInstanceOf(ServiceUnavailableException.class);

        verify(issueRepository, never()).delete(any(Issue.class));
    }

    @Test
    @DisplayName("The project cascade clears every issue's comments before deleting any issue")
    void projectCascadeClearsAllCommentsFirst() {
        when(issueRepository.findIdsByProjectId(1011)).thenReturn(List.of(1, 2, 3));

        service().deleteByProject(1011);

        InOrder order = inOrder(commentGateway, issueRepository);
        order.verify(commentGateway).deleteByIssue(1);
        order.verify(commentGateway).deleteByIssue(2);
        order.verify(commentGateway).deleteByIssue(3);
        order.verify(issueRepository).deleteByProjectId(1011);
    }

    @Test
    @DisplayName("If one issue's comments cannot be cleared, no issue rows are deleted")
    void partialCommentFailureAbortsTheBulkDelete() {
        when(issueRepository.findIdsByProjectId(1011)).thenReturn(List.of(1, 2, 3));

        // lenient(): the cascade calls deleteByIssue(1) before reaching the stubbed
        // deleteByIssue(2), and strict stubbing treats that first call as a mismatch.
        // Failing on the second issue rather than the first is the point of the test -
        // it proves the abort happens part-way through, not only at the very start.
        lenient().doThrow(
                        new ServiceUnavailableException("Comment Service", new RuntimeException()))
                .when(commentGateway).deleteByIssue(2);

        assertThatThrownBy(() -> service().deleteByProject(1011))
                .isInstanceOf(ServiceUnavailableException.class);

        // Deleting the issues anyway would strand issue 2's comments permanently: nothing
        // else in the system would know that issue id had ever existed.
        verify(issueRepository, never()).deleteByProjectId(anyInt());
    }

    @Test
    @DisplayName("A project with no issues is a clean no-op, so the caller can always retry")
    void emptyProjectIsANoOp() {
        when(issueRepository.findIdsByProjectId(1013)).thenReturn(List.of());

        service().deleteByProject(1013);

        verify(commentGateway, never()).deleteByIssue(anyInt());
        verify(issueRepository, never()).deleteByProjectId(anyInt());
    }
}
